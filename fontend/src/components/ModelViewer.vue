<template>
  <div ref="container" class="model-viewer-container"></div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, watch } from "vue"
import * as THREE from "three"
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls"
import { OBJLoader } from "three/examples/jsm/loaders/OBJLoader"

const props = defineProps({
  modelUrl: String,
  expressions: {
    type: Array,
    default: () => [0, 0, 0, 0, 0, 0, 0]
  }
})

const emit = defineEmits(['loadError'])

const container = ref(null)
let scene, camera, renderer, controls, resizeObserver
let currentModel = null
let originalPositions = new Map()
let faceRegion = null // auto-detected face bounding box
let animationId = null
// 平板检测：iPad Safari 桌面模式 UA 含 "Macintosh" 但支持触摸，需要额外判断
const isTablet = typeof navigator !== 'undefined' && (
  /iPad|Android|Tablet/i.test(navigator.userAgent) ||
  (navigator.maxTouchPoints > 1 && /Macintosh/i.test(navigator.userAgent))
)

onMounted(() => {
  initScene()
  if (props.modelUrl) loadModel(props.modelUrl)
})

onBeforeUnmount(() => {
  if (animationId) { cancelAnimationFrame(animationId); animationId = null }
  if (resizeObserver) { resizeObserver.disconnect(); resizeObserver = null }
  if (controls) { controls.dispose(); controls = null }
  if (renderer) {
    renderer.dispose()
    renderer.forceContextLoss()
    if (renderer.domElement && renderer.domElement.parentNode) {
      renderer.domElement.parentNode.removeChild(renderer.domElement)
    }
    renderer = null
  }
  if (currentModel && scene) {
    scene.remove(currentModel)
    currentModel = null
  }
  if (scene) {
    scene.traverse((child) => {
      if (child.geometry) child.geometry.dispose()
      if (child.material) {
        if (Array.isArray(child.material)) child.material.forEach(m => m.dispose())
        else child.material.dispose()
      }
    })
    scene = null
  }
  camera = null
  originalPositions.clear()
})

watch(() => props.modelUrl, (url) => { if (url) loadModel(url) })
watch(() => props.expressions, (e) => { if (currentModel && e) applyDeformation(e) }, { deep: true })

function initScene() {
  const el = container.value
  if (!el) return
  const w = el.clientWidth || 400
  const h = el.clientHeight || 400

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x1a1a2e)

  camera = new THREE.PerspectiveCamera(45, w / h, 0.1, 1000)
  camera.position.set(0, 0, 3)

  // 平板限制 DPR 和关闭抗锯齿，防止 GPU 内存不足导致白屏/闪退
  const maxPR = isTablet ? 1.5 : 2
  try {
    renderer = new THREE.WebGLRenderer({
      antialias: !isTablet,
      powerPreference: 'default',
      failIfMajorPerformanceCaveat: false
    })
  } catch (e) {
    console.error('ModelViewer WebGL 初始化失败:', e)
    return
  }
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, maxPR))
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.2
  el.appendChild(renderer.domElement)

  // WebGL context lost 处理
  renderer.domElement.addEventListener('webglcontextlost', (e) => {
    e.preventDefault()
    console.warn('ModelViewer: WebGL context lost')
    if (animationId) { cancelAnimationFrame(animationId); animationId = null }
  })
  renderer.domElement.addEventListener('webglcontextrestored', () => {
    console.log('ModelViewer: WebGL context restored')
    animate()
  })

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.08

  scene.add(new THREE.AmbientLight(0x404060, 0.5))
  scene.add(new THREE.HemisphereLight(0xddeeff, 0x0d0d0d, 0.8))
  const key = new THREE.DirectionalLight(0xffffff, 1.0)
  key.position.set(5, 10, 7)
  scene.add(key)
  const fill = new THREE.DirectionalLight(0x8888ff, 0.3)
  fill.position.set(-5, 0, -5)
  scene.add(fill)

  const grid = new THREE.GridHelper(4, 20, 0x333355, 0x222244)
  grid.position.y = -1.2
  scene.add(grid)

  resizeObserver = new ResizeObserver(() => {
    const nw = el.clientWidth
    const nh = el.clientHeight
    if (nw > 0 && nh > 0) {
      camera.aspect = nw / nh
      camera.updateProjectionMatrix()
      renderer.setSize(nw, nh)
    }
  })
  resizeObserver.observe(el)

  animate()
}

/**
 * Auto-detect face region by finding the cluster of vertices
 * with the highest Z values (most forward-facing).
 * Returns { yMin, yMax, xMin, xMax, zThreshold, centerX, centerY }
 */
function detectFaceRegion(allVertices) {
  if (allVertices.length === 0) return null

  // Sort by Z descending to find front-facing vertices
  const sorted = [...allVertices].sort((a, b) => b.z - a.z)

  // Take top 15% of forward-facing vertices as "face candidates"
  const topCount = Math.max(50, Math.floor(sorted.length * 0.15))
  const faceVerts = sorted.slice(0, topCount)

  let yMin = Infinity, yMax = -Infinity
  let xMin = Infinity, xMax = -Infinity
  let zMin = Infinity
  let sumX = 0, sumY = 0

  for (const v of faceVerts) {
    if (v.y < yMin) yMin = v.y
    if (v.y > yMax) yMax = v.y
    if (v.x < xMin) xMin = v.x
    if (v.x > xMax) xMax = v.x
    if (v.z < zMin) zMin = v.z
    sumX += v.x
    sumY += v.y
  }

  const centerX = sumX / faceVerts.length
  const centerY = sumY / faceVerts.length

  // Expand bounds slightly for smooth falloff
  const yPad = (yMax - yMin) * 0.1
  const xPad = (xMax - xMin) * 0.1

  return {
    yMin: yMin - yPad,
    yMax: yMax + yPad,
    xMin: xMin - xPad,
    xMax: xMax + xPad,
    zThreshold: zMin,
    centerX,
    centerY
  }
}

function setupLoadedModel(object) {
  if (currentModel && scene) {
    scene.remove(currentModel)
    originalPositions.clear()
  }

  object.traverse((child) => {
    if (child.isMesh) {
      child.material = new THREE.MeshStandardMaterial({
        color: 0xccbbaa, roughness: 0.6, metalness: 0.1,
      })
    }
  })

  // Center & scale
  const box = new THREE.Box3().setFromObject(object)
  const center = box.getCenter(new THREE.Vector3())
  object.position.sub(center)
  const size = box.getSize(new THREE.Vector3()).length()
  object.scale.setScalar(2 / size)

  if (!scene) return
  scene.add(object)
  currentModel = object

  // Store original positions
  object.updateMatrixWorld(true)
  const allVerts = []
  object.traverse((child) => {
    if (child.isMesh && child.geometry) {
      const pos = child.geometry.attributes.position
      originalPositions.set(child, new Float32Array(pos.array))
      for (let i = 0; i < pos.array.length; i += 3) {
        allVerts.push({ x: pos.array[i], y: pos.array[i+1], z: pos.array[i+2] })
      }
    }
  })

  // Auto-detect face
  faceRegion = detectFaceRegion(allVerts)
  if (faceRegion && camera && controls) {
    const faceCY = faceRegion.centerY
    camera.position.set(0, faceCY, 2.0)
    controls.target.set(0, faceCY, 0)
    controls.update()
  } else if (camera && controls) {
    camera.position.set(0, 0, 3)
    controls.target.set(0, 0, 0)
    controls.update()
  }

  if (props.expressions) applyDeformation(props.expressions)
}

function loadModel(objUrl) {
  if (!scene) return

  const loader = new OBJLoader()
  loader.load(objUrl, (object) => {
    setupLoadedModel(object)
  }, undefined, (err) => {
    console.error("Model load error:", err)
    emit('loadError', { url: objUrl, error: err })
  })
}

function applyDeformation(expressions) {
  if (!currentModel || !faceRegion) return

  const fr = faceRegion
  const faceHeight = fr.yMax - fr.yMin
  const faceWidth = fr.xMax - fr.xMin

  // 7 regions relative to face bounding box
  // Positions are fractions within the face region (0=bottom, 1=top for Y; -1 to 1 for X)
  const regions = [
    { name: 'Forehead',    fy: 0.85, fx: 0.0,   ry: 0.12, rx: 0.35, axis: 'z' },
    { name: 'Left Eye',    fy: 0.65, fx: -0.25,  ry: 0.06, rx: 0.12, axis: 'z' },
    { name: 'Right Eye',   fy: 0.65, fx: 0.25,   ry: 0.06, rx: 0.12, axis: 'z' },
    { name: 'Nose',        fy: 0.45, fx: 0.0,    ry: 0.12, rx: 0.08, axis: 'z' },
    { name: 'Mouth',       fy: 0.25, fx: 0.0,    ry: 0.06, rx: 0.15, axis: 'z' },
    { name: 'Left Cheek',  fy: 0.50, fx: -0.40,  ry: 0.12, rx: 0.12, axis: 'x' },
    { name: 'Right Cheek', fy: 0.50, fx: 0.40,   ry: 0.12, rx: 0.12, axis: 'x' },
  ]

  currentModel.traverse((child) => {
    if (!child.isMesh || !child.geometry) return
    const origArray = originalPositions.get(child)
    if (!origArray) return

    const posAttr = child.geometry.attributes.position
    const arr = posAttr.array

    // Reset to original
    arr.set(origArray)

    for (let i = 0; i < origArray.length; i += 3) {
      const ox = origArray[i]
      const oy = origArray[i + 1]
      const oz = origArray[i + 2]

      // Skip vertices not in face region (with some margin)
      if (oy < fr.yMin || oy > fr.yMax) continue
      if (ox < fr.xMin || ox > fr.xMax) continue
      if (oz < fr.zThreshold) continue // behind face

      // Normalize within face region
      const ny = (oy - fr.yMin) / faceHeight  // 0~1
      const nx = (ox - fr.centerX) / (faceWidth / 2) // -1~1

      for (let r = 0; r < regions.length && r < expressions.length; r++) {
        const val = expressions[r] // already display value: -4 to +6
        if (val === 0) continue

        const reg = regions[r]

        // Ellipse distance
        const dx = (nx - reg.fx) / reg.rx
        const dy = (ny - reg.fy) / reg.ry
        const dist2 = dx * dx + dy * dy

        if (dist2 > 1.0) continue

        // Smooth gaussian falloff
        const weight = Math.exp(-dist2 * 2.0)

        // Strength: each unit = small displacement
        // val ranges -4 to +6, so max displacement = 6 * 0.02 * faceHeight
        const strength = val * 0.015 * faceHeight * weight

        if (reg.axis === 'z') {
          arr[i + 2] += strength
        } else if (reg.axis === 'x') {
          const sign = ox > fr.centerX ? 1 : -1
          arr[i] += strength * sign
        }
      }
    }

    posAttr.needsUpdate = true
    child.geometry.computeVertexNormals()
  })
}

function animate() {
  if (!renderer || !scene || !camera) return
  animationId = requestAnimationFrame(animate)
  if (controls) controls.update()
  renderer.render(scene, camera)
}
</script>

<style scoped>
.model-viewer-container {
  width: 100%;
  height: 100%;
  min-height: 300px;
}
</style>