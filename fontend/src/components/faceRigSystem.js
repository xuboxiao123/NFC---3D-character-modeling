/**
 * 面部骨骼自动绑定系统
 * 实现: 形态键(Shape Keys) + 骨骼(Bones) + 驱动器(Drivers)
 *
 * 精简版：将22个参数精简到12个核心参数，合并冗余控制
 * 修复：头发区域跟随面部变形，避免脱离
 */
import * as THREE from 'three'

// 骨骼定义: [id, relativePos[x,y,z](归一化到面部区域), parentId]
// X: 0=左 1=右, Y: 0=下巴底部 1=额头顶, Z: 0=后脑 1=脸前方
export const BONE_DEFS = [
  ['root', [0.5, 0.5, 0.5], null],
  ['face_center', [0.5, 0.50, 0.75], 'root'],
  // 头顶/头发区域骨骼 - 确保头发跟随脸部变形
  ['head_top', [0.5, 0.92, 0.55], 'face_center'],
  ['head_left', [0.25, 0.85, 0.50], 'face_center'],
  ['head_right', [0.75, 0.85, 0.50], 'face_center'],
  ['jaw_left', [0.28, 0.20, 0.65], 'face_center'],
  ['jaw_right', [0.72, 0.20, 0.65], 'face_center'],
  ['cheek_left', [0.22, 0.45, 0.72], 'face_center'],
  ['cheek_right', [0.78, 0.45, 0.72], 'face_center'],
  ['chin', [0.5, 0.08, 0.78], 'face_center'],
  ['eye_left', [0.35, 0.58, 0.82], 'face_center'],
  ['eye_right', [0.65, 0.58, 0.82], 'face_center'],
  ['nose_bridge', [0.5, 0.50, 0.90], 'face_center'],
  ['nose_tip', [0.5, 0.38, 0.95], 'nose_bridge'],
  ['nose_left', [0.42, 0.36, 0.88], 'nose_tip'],
  ['nose_right', [0.58, 0.36, 0.88], 'nose_tip'],
  ['mouth_center', [0.5, 0.22, 0.82], 'face_center'],
  ['mouth_left', [0.38, 0.22, 0.78], 'mouth_center'],
  ['mouth_right', [0.62, 0.22, 0.78], 'mouth_center'],
  ['lip_upper', [0.5, 0.25, 0.85], 'mouth_center'],
  ['lip_lower', [0.5, 0.19, 0.83], 'mouth_center'],
  ['forehead_center', [0.5, 0.78, 0.80], 'face_center'],
]

// 驱动器: paramId -> [[boneId, type, axis, relativeFactor], ...]
// relativeFactor: 相对于头部尺寸的比例
// 精简后的12个参数，每个参数同时驱动面部和头发骨骼以保持一致性
export const DRIVERS = {
  // === 脸型 (4个) ===
  faceWidth: [
    // 面部骨骼
    ['cheek_left','translate','x',-0.10], ['cheek_right','translate','x',0.10],
    ['jaw_left','translate','x',-0.08], ['jaw_right','translate','x',0.08],
    // 头发/头顶骨骼 - 跟随脸宽联动，避免头发脱离
    ['head_left','translate','x',-0.06], ['head_right','translate','x',0.06],
    ['forehead_center','scale','x',0.08],
  ],
  jawWidth: [
    ['jaw_left','translate','x',-0.08], ['jaw_right','translate','x',0.08],
    ['cheek_left','translate','x',-0.03], ['cheek_right','translate','x',0.03],
  ],
  chinLength: [
    ['chin','translate','y',-0.08],
  ],
  cheekBone: [
    ['cheek_left','translate','x',-0.12], ['cheek_right','translate','x',0.12],
    ['cheek_left','translate','z',0.08], ['cheek_right','translate','z',0.08],
    // 头发侧面微调跟随
    ['head_left','translate','x',-0.04], ['head_right','translate','x',0.04],
  ],

  // === 眼睛 (2个) ===
  eyeSize: [
    ['eye_left','scale','x',0.25], ['eye_left','scale','y',0.25],
    ['eye_right','scale','x',0.25], ['eye_right','scale','y',0.25],
  ],
  eyeDistance: [
    ['eye_left','translate','x',-0.04], ['eye_right','translate','x',0.04],
  ],

  // === 鼻子 (2个) ===
  noseHeight: [
    ['nose_bridge','translate','z',0.08],
    ['nose_tip','translate','z',0.04],
  ],
  noseWidth: [
    ['nose_left','translate','x',-0.06], ['nose_right','translate','x',0.06],
  ],

  // === 嘴巴 (2个) ===
  mouthWidth: [
    ['mouth_left','translate','x',-0.06], ['mouth_right','translate','x',0.06],
    // 嘴角上扬/下拉：正值时嘴角向上（微笑），负值时嘴角向下（悲伤）
    ['mouth_left','translate','y',0.03], ['mouth_right','translate','y',0.03],
  ],
  lipThickness: [
    ['lip_upper','translate','z',0.03], ['lip_upper','translate','y',0.02],
    ['lip_lower','translate','z',0.03], ['lip_lower','translate','y',-0.02],
  ],

  // === 额头 (2个) ===
  foreheadHeight: [
    ['forehead_center','translate','y',0.06],
    // 头顶跟随额头变化
    ['head_top','translate','y',0.04],
  ],
  foreheadDepth: [
    ['forehead_center','translate','z',0.05],
  ],

  // === NFC 单侧参数 (4个) ===
  // 仅驱动单侧骨骼，与对称参数(eyeSize/cheekBone)效果叠加
  eyeSizeLeft: [
    ['eye_left','scale','x',0.25], ['eye_left','scale','y',0.25],
  ],
  eyeSizeRight: [
    ['eye_right','scale','x',0.25], ['eye_right','scale','y',0.25],
  ],
  cheekLeft: [
    ['cheek_left','translate','x',-0.14],
    ['cheek_left','translate','z',0.10],
    ['jaw_left','translate','x',-0.05],
    ['head_left','translate','x',-0.04],
  ],
  cheekRight: [
    ['cheek_right','translate','x',0.14],
    ['cheek_right','translate','z',0.10],
    ['jaw_right','translate','x',0.05],
    ['head_right','translate','x',0.04],
  ],
}

// 形态键权重系数
export const SHAPE_WEIGHTS = {
  faceWidth: 0.15, jawWidth: 0.2, cheekBone: 0.35,
  chinLength: 0.15,
  eyeSize: 0.25, eyeDistance: 0.15,
  noseHeight: 0.25, noseWidth: 0.2,
  mouthWidth: 0.2, lipThickness: 0.2,
  foreheadHeight: 0.15, foreheadDepth: 0.2,
  // NFC 单侧参数
  eyeSizeLeft: 0.25, eyeSizeRight: 0.25,
  cheekLeft: 0.35, cheekRight: 0.35,
}

// 面部参数定义 (12个核心参数 + 4个NFC单侧参数)
export const FACE_PARAMS_DEF = [
  { id: 'faceWidth', name: 'Face Width', category: 'face', min: -1, max: 1 },
  { id: 'jawWidth', name: 'Jaw Width', category: 'face', min: -1, max: 1 },
  { id: 'chinLength', name: 'Chin Length', category: 'mouth', min: -1, max: 1 },
  { id: 'cheekBone', name: 'Cheekbone', category: 'face', min: -1, max: 1 },
  { id: 'eyeSize', name: 'Eye Size', category: 'eyes', min: -1, max: 1 },
  { id: 'eyeDistance', name: 'Eye Distance', category: 'eyes', min: -1, max: 1 },
  { id: 'noseHeight', name: 'Nose Bridge', category: 'nose', min: -1, max: 1 },
  { id: 'noseWidth', name: 'Nose Width', category: 'nose', min: -1, max: 1 },
  { id: 'mouthWidth', name: 'Mouth Width', category: 'mouth', min: -1, max: 1 },
  { id: 'lipThickness', name: 'Lip Thickness', category: 'mouth', min: -1, max: 1 },
  { id: 'foreheadHeight', name: 'Forehead Height', category: 'face', min: -1, max: 1 },
  { id: 'foreheadDepth', name: 'Forehead Depth', category: 'face', min: -1, max: 1 },
  // NFC 单侧参数 — 由实体 NFC 装置独立控制左右两侧（归入对应面部分类）
  { id: 'eyeSizeLeft', name: 'Left Eye Size', category: 'eyes', min: -1, max: 1 },
  { id: 'eyeSizeRight', name: 'Right Eye Size', category: 'eyes', min: -1, max: 1 },
  { id: 'cheekLeft', name: 'Left Cheek', category: 'face', min: -1, max: 1 },
  { id: 'cheekRight', name: 'Right Cheek', category: 'face', min: -1, max: 1 },
]

// 手动锚点定义: 用户需要在模型上依次标记的7个关键点（与 NFC 实体装置的7个面部细节对应）
export const ANCHOR_DEFS = [
  { id: 'forehead',    name: 'Forehead',    icon: '🧠', color: 0xffff44, hint: 'Click the center of the forehead' },
  { id: 'eyeLeft',     name: 'Left Eye',    icon: '👁️', color: 0x44aaff, hint: 'Click the center of the left eye' },
  { id: 'eyeRight',    name: 'Right Eye',   icon: '👁️', color: 0x44aaff, hint: 'Click the center of the right eye' },
  { id: 'noseTip',     name: 'Nose Tip',    icon: '👃', color: 0x44ff44, hint: 'Click the nose tip' },
  { id: 'mouthCenter', name: 'Mouth',       icon: '👄', color: 0xff44ff, hint: 'Click the center of the mouth' },
  { id: 'cheekLeft',   name: 'Left Cheek',  icon: '🫲', color: 0xffaa44, hint: 'Click the raised area of the left cheek' },
  { id: 'cheekRight',  name: 'Right Cheek', icon: '🫱', color: 0xffaa44, hint: 'Click the raised area of the right cheek' },
]

// NFC 读卡器索引 → faceParam 参数名 映射
// 根据实际物理安装位置重新映射：
// reader 0=右脸颊, 1=下巴, 2=左脸颊, 3=左眼, 4=额头, 5=右眼
export const NFC_PARAM_MAPPING = [
  'cheekRight',      // reader 0 → 右脸颊
  'chinLength',      // reader 1 → 下巴
  'cheekLeft',       // reader 2 → 左脸颊
  'eyeSizeLeft',     // reader 3 → 左眼大小
  'foreheadHeight',  // reader 4 → 额头高度
  'eyeSizeRight',    // reader 5 → 右眼大小
]

// expression 值 (整数 1~11, 中性=5) 与 faceParam 值 (浮点 -1.0~1.0) 的互转
// 数据库范围 1~11，中间值5代表0
// 每刷一次卡 expression ±1 → faceParam ±0.1667 (1/6)
export function expressionToFaceParam(exprValue) {
  return Math.max(-1, Math.min(1, (exprValue - 5) / 6.0))
}

export function faceParamToExpression(paramValue) {
  return Math.max(1, Math.min(11, Math.round(paramValue * 6.0 + 5)))
}

// ===== 表情预设 (E01~E05) — 参数对应 NfcPresetService =====
export const EXPRESSION_PRESETS = [
  {
    code: 'E01', name: 'HAPPY', image: '/presets/happy.png',
    values: { faceWidth: 0, jawWidth: 0, chinLength: 0, cheekBone: 0.1, eyeSize: -0.1, eyeDistance: 0, noseHeight: 0, noseWidth: 0, mouthWidth: 0.4, lipThickness: 0.1, foreheadHeight: 0, foreheadDepth: 0, eyeSizeLeft: -0.05, eyeSizeRight: -0.05, cheekLeft: 0, cheekRight: 0 }
  },
  {
    code: 'E02', name: 'AMAZED', image: '/presets/amazed.png',
    values: { faceWidth: 0, jawWidth: 0, chinLength: 0, cheekBone: -0.5, eyeSize: 0.3, eyeDistance: 0, noseHeight: 0, noseWidth: 0, mouthWidth: -0.5, lipThickness: -0.35, foreheadHeight: 0.35, foreheadDepth: 0, eyeSizeLeft: 0.25, eyeSizeRight: 0.25, cheekLeft: 0, cheekRight: 0 }
  },
  {
    code: 'E03', name: 'ANGRY', image: '/presets/angry.png',
    values: { faceWidth: 0.2, jawWidth: 0.4, chinLength: 0, cheekBone: 0.2, eyeSize: 0.2, eyeDistance: 0, noseHeight: 0, noseWidth: 0.35, mouthWidth: -0.5, lipThickness: -0.35, foreheadHeight: -0.6, foreheadDepth: 0.7, eyeSizeLeft: 0.1, eyeSizeRight: 0.1, cheekLeft: 0, cheekRight: 0 }
  },
  {
    code: 'E04', name: 'SAD', image: '/presets/sad.png',
    values: { faceWidth: 0.1, jawWidth: 0.2, chinLength: 0, cheekBone: 0.15, eyeSize: -0.35, eyeDistance: 0, noseHeight: 0, noseWidth: 0.1, mouthWidth: -0.35, lipThickness: -0.25, foreheadHeight: -0.25, foreheadDepth: 0.35, eyeSizeLeft: -0.3, eyeSizeRight: -0.3, cheekLeft: 0, cheekRight: 0 }
  },
  {
    code: 'E05', name: 'SPEECHLESS', image: '/presets/speechless.png',
    values: { faceWidth: 0.15, jawWidth: 0.3, chinLength: 0, cheekBone: 0.2, eyeSize: -0.15, eyeDistance: 0, noseHeight: 0, noseWidth: 0.15, mouthWidth: 0.2, lipThickness: -0.35, foreheadHeight: -0.3, foreheadDepth: 0.4, eyeSizeLeft: -0.1, eyeSizeRight: -0.1, cheekLeft: 0, cheekRight: 0 }
  },
]

// ===== 脸型预设 (F01~F05) — 参数对应 NfcPresetService =====
export const FACE_PRESETS = [
  {
    code: 'F01', name: 'Round', image: '/presets/round-face.png',
    values: { faceWidth: 0.5, jawWidth: 0.3, cheekBone: 0.2, chinLength: -0.2 }
  },
  {
    code: 'F02', name: 'Oval', image: '/presets/oval-face.png',
    values: { faceWidth: -0.2, jawWidth: -0.5, chinLength: 0.3 }
  },
  {
    code: 'F03', name: 'Square', image: '/presets/square-face.png',
    values: { faceWidth: 0.5, jawWidth: 0.8, cheekBone: -0.1, chinLength: -0.5, foreheadHeight: -0.1, foreheadDepth: 0.15 }
  },
  {
    code: 'F04', name: 'Big Eye', image: '/presets/big-eye-face.png',
    values: { eyeSize: 0.7, eyeDistance: 0.1, noseWidth: -0.2, chinLength: -0.2, faceWidth: 0.1 }
  },
  {
    code: 'F05', name: 'Elf', image: '/presets/elf-face.png',
    values: { faceWidth: -0.3, chinLength: 0.4, eyeSize: 0.3, noseHeight: 0.4, foreheadHeight: 0.2 }
  },
]

// 兼容旧引用：合并为 PRESETS
export const PRESETS = [...EXPRESSION_PRESETS, ...FACE_PRESETS]

/**
 * FaceRigSystem - 面部骨骼绑定系统类
 */
export class FaceRigSystem {
  constructor() {
    this.boneMap = {}
    this.boneRestPos = {}    // boneId -> Vector3 (模型局部坐标)
    this.boneRestScale = {}
    this.skeleton = null
    this.skinnedMeshEntries = []
    this.shapeKeyDeltas = new Map()  // paramId -> Map(meshUuid -> Float32Array)
    this.headBBox = null     // 头部区域包围盒（用于骨骼定位）
    this.headSize = null
    this.headTransitionBBox = null  // 头部过渡区域包围盒（平滑衰减）
    this.modelBBox = null
    this.modelSize = null
    this.boneHelperGroup = null
  }

  /**
   * 基于用户手动标记的锚点来推算头部区域
   * anchors: { forehead: Vector3, eyeLeft: Vector3, eyeRight: Vector3, noseTip: Vector3, mouthCenter: Vector3, cheekLeft: Vector3, cheekRight: Vector3 }
   */
  detectHeadRegionFromAnchors(anchors) {
    // 用锚点推算头部包围盒
    const points = Object.values(anchors)
    const min = new THREE.Vector3(Infinity, Infinity, Infinity)
    const max = new THREE.Vector3(-Infinity, -Infinity, -Infinity)
    for (const p of points) {
      min.min(p)
      max.max(p)
    }

    // 基于锚点范围扩展包围盒
    const spanY = max.y - min.y
    const spanX = max.x - min.x
    const spanZ = max.z - min.z

    // 额头上方还有头顶，扩展约 50%
    max.y += spanY * 0.5
    // 嘴巴下方还有下巴，扩展约 30%
    min.y -= spanY * 0.3
    // X方向（左右）：仅微扩15%，防止把手臂包含进来
    min.x -= spanX * 0.15
    max.x += spanX * 0.15
    // Z方向（前后）：后方扩展用于头发，前方微扩
    min.z -= spanZ * 0.3
    max.z += spanZ * 0.1

    this.headBBox = new THREE.Box3(min, max)
    this.headSize = this.headBBox.getSize(new THREE.Vector3())
    // 过渡区域包围盒：在头部包围盒基础上外扩一圈，用于平滑过渡
    const transitionPad = Math.max(spanY, spanX) * 0.15
    this.headTransitionBBox = new THREE.Box3(
      min.clone().subScalar(transitionPad),
      max.clone().addScalar(transitionPad)
    )

    console.log(`🧠 锚点定位头部: Y[${min.y.toFixed(3)}, ${max.y.toFixed(3)}], ` +
                `X[${min.x.toFixed(3)}, ${max.x.toFixed(3)}], ` +
                `高度=${this.headSize.y.toFixed(3)}, 宽度=${this.headSize.x.toFixed(3)}`)
    return this.headBBox
  }

  /**
   * 基于锚点推算骨骼位置（覆盖默认的相对坐标定位）
   * 使用7个锚点：额头、左眼、右眼、鼻尖、嘴巴、左脸颊、右脸颊
   * 返回 boneId -> Vector3 的映射
   */
  computeBonePositionsFromAnchors(anchors) {
    const { eyeLeft, eyeRight, noseTip, mouthCenter, forehead, cheekLeft, cheekRight } = anchors
    const eyeCenter = eyeLeft.clone().add(eyeRight).multiplyScalar(0.5)
    const faceCenter = eyeCenter.clone().add(noseTip).multiplyScalar(0.5)

    // 面部朝向: 从 faceCenter 到 noseTip 的方向约为 Z 正方向
    const faceForward = noseTip.clone().sub(faceCenter).normalize()
    // 面部上方向: 从 mouthCenter 到 forehead
    const faceUp = forehead.clone().sub(mouthCenter).normalize()
    // 面部右方向: 优先利用用户标记的左右脸颊来确定
    let faceRight
    if (cheekLeft && cheekRight) {
      // 使用左右脸颊锚点的方向作为面部横向基准（右减左得到右方向）
      faceRight = cheekRight.clone().sub(cheekLeft).normalize()
    } else {
      faceRight = new THREE.Vector3().crossVectors(faceUp, faceForward).normalize()
    }

    const eyeDist = eyeLeft.distanceTo(eyeRight)
    const faceHeight = forehead.distanceTo(mouthCenter)

    // 根据锚点推算各骨骼的绝对位置
    const positions = {}
    positions['root'] = faceCenter.clone()
    positions['face_center'] = faceCenter.clone()

    // 头顶
    positions['head_top'] = forehead.clone().add(faceUp.clone().multiplyScalar(faceHeight * 0.5))
    positions['head_left'] = forehead.clone().add(faceRight.clone().multiplyScalar(-eyeDist * 0.8))
      .add(faceUp.clone().multiplyScalar(faceHeight * 0.2))
    positions['head_right'] = forehead.clone().add(faceRight.clone().multiplyScalar(eyeDist * 0.8))
      .add(faceUp.clone().multiplyScalar(faceHeight * 0.2))

    // 下巴区域
    const chinPos = mouthCenter.clone().add(faceUp.clone().multiplyScalar(-faceHeight * 0.3))
    positions['chin'] = chinPos
    positions['jaw_left'] = chinPos.clone().add(faceRight.clone().multiplyScalar(-eyeDist * 0.5))
      .add(faceUp.clone().multiplyScalar(faceHeight * 0.1))
    positions['jaw_right'] = chinPos.clone().add(faceRight.clone().multiplyScalar(eyeDist * 0.5))
      .add(faceUp.clone().multiplyScalar(faceHeight * 0.1))

    // 脸颊 — 优先使用用户手动标记的位置
    if (cheekLeft) {
      positions['cheek_left'] = cheekLeft.clone()
    } else {
      positions['cheek_left'] = eyeLeft.clone().add(faceRight.clone().multiplyScalar(-eyeDist * 0.3))
        .add(faceUp.clone().multiplyScalar(-faceHeight * 0.15))
    }
    if (cheekRight) {
      positions['cheek_right'] = cheekRight.clone()
    } else {
      positions['cheek_right'] = eyeRight.clone().add(faceRight.clone().multiplyScalar(eyeDist * 0.3))
        .add(faceUp.clone().multiplyScalar(-faceHeight * 0.15))
    }

    // 眼睛
    positions['eye_left'] = eyeLeft.clone()
    positions['eye_right'] = eyeRight.clone()

    // 鼻子
    const noseBridge = eyeCenter.clone().lerp(noseTip, 0.4)
    positions['nose_bridge'] = noseBridge
    positions['nose_tip'] = noseTip.clone()
    positions['nose_left'] = noseTip.clone().add(faceRight.clone().multiplyScalar(-eyeDist * 0.15))
    positions['nose_right'] = noseTip.clone().add(faceRight.clone().multiplyScalar(eyeDist * 0.15))

    // 嘴巴
    positions['mouth_center'] = mouthCenter.clone()
    positions['mouth_left'] = mouthCenter.clone().add(faceRight.clone().multiplyScalar(-eyeDist * 0.25))
    positions['mouth_right'] = mouthCenter.clone().add(faceRight.clone().multiplyScalar(eyeDist * 0.25))
    positions['lip_upper'] = mouthCenter.clone().add(faceUp.clone().multiplyScalar(faceHeight * 0.03))
      .add(faceForward.clone().multiplyScalar(eyeDist * 0.05))
    positions['lip_lower'] = mouthCenter.clone().add(faceUp.clone().multiplyScalar(-faceHeight * 0.03))
      .add(faceForward.clone().multiplyScalar(eyeDist * 0.03))

    // 额头
    positions['forehead_center'] = forehead.clone()

    return positions
  }

  /**
   * 基于用户手动标记的锚点进行骨骼绑定
   * anchors: { forehead: Vector3, eyeLeft: Vector3, eyeRight: Vector3, noseTip: Vector3, mouthCenter: Vector3, cheekLeft: Vector3, cheekRight: Vector3 }
   */
  autoBindWithAnchors(model, bbox, size, anchors) {
    this.destroy()
    this.modelBBox = bbox
    this.modelSize = size
    model.updateMatrixWorld(true)

    // 1. 用锚点推算头部区域
    const headDetected = this.detectHeadRegionFromAnchors(anchors)
    if (!headDetected) {
      console.error('❌ 锚点定位头部失败')
      return
    }

    // 2. 用锚点推算骨骼位置
    const anchorPositions = this.computeBonePositionsFromAnchors(anchors)

    // 3. 创建骨骼
    const allBones = []
    this.boneMap = {}
    this.boneRestPos = {}
    this.boneRestScale = {}

    for (const [id, rel] of BONE_DEFS) {
      const bone = new THREE.Bone()
      bone.name = id
      // 优先使用锚点推算的位置，否则降级到头部区域相对坐标
      const localPos = anchorPositions[id]
        ? anchorPositions[id].clone()
        : this.relToHead(rel)
      this.boneMap[id] = bone
      this.boneRestPos[id] = localPos.clone()
      this.boneRestScale[id] = new THREE.Vector3(1, 1, 1)
      allBones.push(bone)
    }

    // 4. 建立父子关系
    for (const [id, , parentId] of BONE_DEFS) {
      if (parentId && this.boneMap[parentId]) {
        this.boneMap[parentId].add(this.boneMap[id])
        const pPos = this.boneRestPos[parentId]
        const cPos = this.boneRestPos[id]
        this.boneMap[id].position.copy(cPos.clone().sub(pPos))
      } else if (!parentId) {
        this.boneMap[id].position.copy(this.boneRestPos[id])
      }
    }

    model.add(this.boneMap['root'])
    this.skeleton = new THREE.Skeleton(allBones)

    // 5. 收集 Mesh 并进行蒙皮绑定（复用 autoBind 中相同的逻辑）
    const meshes = []
    model.traverse((child) => {
      if (child.isMesh && child.geometry) meshes.push(child)
    })

    // 计算影响半径
    const bonePosArr = BONE_DEFS
      .filter(d => d[0] !== 'root')
      .map(d => this.boneRestPos[d[0]])
    let avgDist = 0, cnt = 0
    for (let a = 0; a < bonePosArr.length; a++) {
      for (let b = a + 1; b < bonePosArr.length; b++) {
        avgDist += bonePosArr[a].distanceTo(bonePosArr[b])
        cnt++
      }
    }
    avgDist = cnt > 0 ? avgDist / cnt : 0.1
    const influenceR = Math.max(avgDist * 0.8, this.headSize.y * 0.3)

    console.log(`🦴 锚点绑定 - 影响半径: ${influenceR.toFixed(4)}, 头部尺寸: ${this.headSize.y.toFixed(4)}`)

    // 6. 转换为 SkinnedMesh（复用原 autoBind 的蒙皮权重计算逻辑）
    let isFirst = true
    this.skinnedMeshEntries = []

    for (const mesh of meshes) {
      const geo = mesh.geometry.clone()
      const vCount = geo.attributes.position.count
      const posArr = geo.attributes.position.array
      const skinIdx = new Float32Array(vCount * 4)
      const skinWgt = new Float32Array(vCount * 4)

      // 使用世界矩阵将顶点变换到世界空间（与射线交点/锚点坐标一致）
      mesh.updateMatrixWorld(true)
      const meshWorldMat = mesh.matrixWorld.clone()

      for (let v = 0; v < vCount; v++) {
        const vtx = new THREE.Vector3(
          posArr[v * 3], posArr[v * 3 + 1], posArr[v * 3 + 2]
        ).applyMatrix4(meshWorldMat)

        const headWeight = this.headInfluence(vtx)

        if (headWeight < 0.001) {
          skinIdx[v * 4] = 0
          skinIdx[v * 4 + 1] = 0
          skinIdx[v * 4 + 2] = 0
          skinIdx[v * 4 + 3] = 0
          skinWgt[v * 4] = 1
          skinWgt[v * 4 + 1] = 0
          skinWgt[v * 4 + 2] = 0
          skinWgt[v * 4 + 3] = 0
          continue
        }

        const dists = allBones.map((bone, idx) => {
          if (bone.name === 'root') return { idx, d: Infinity }
          return { idx, d: vtx.distanceTo(this.boneRestPos[bone.name]) }
        })
        dists.sort((a, b) => a.d - b.d)
        const top4 = dists.slice(0, 4)

        let weights = []
        let wSum = 0
        for (let k = 0; k < 4; k++) {
          if (k < top4.length && top4[k].d < influenceR) {
            const t = Math.max(0, 1 - top4[k].d / influenceR)
            const w = t * t * t * (t * (t * 6 - 15) + 10)
            weights.push(w)
            wSum += w
          } else {
            weights.push(0)
          }
        }

        if (wSum > 0.001) {
          weights = weights.map(w => w / wSum)
          if (headWeight < 1.0) {
            const rootWeight = 1.0 - headWeight
            for (let k = 0; k < 4; k++) {
              weights[k] *= headWeight
            }
            let rootSlot = top4.findIndex(d => d.idx === 0)
            if (rootSlot === -1) {
              rootSlot = 3
              top4[3] = { idx: 0 }
            }
            weights[rootSlot] += rootWeight
            wSum = weights.reduce((a, b) => a + b, 0)
            if (wSum > 0) weights = weights.map(w => w / wSum)
          }
        } else {
          weights = [1, 0, 0, 0]
          top4[0] = { idx: 0 }
        }

        for (let k = 0; k < 4; k++) {
          skinIdx[v * 4 + k] = k < top4.length ? top4[k].idx : 0
          skinWgt[v * 4 + k] = weights[k]
        }
      }

      geo.setAttribute('skinIndex', new THREE.Uint16BufferAttribute(skinIdx, 4))
      geo.setAttribute('skinWeight', new THREE.Float32BufferAttribute(skinWgt, 4))

      const sMat = mesh.material.clone()
      const sMesh = new THREE.SkinnedMesh(geo, sMat)
      sMesh.position.copy(mesh.position)
      sMesh.rotation.copy(mesh.rotation)
      sMesh.scale.copy(mesh.scale)
      sMesh.name = mesh.name + '_skinned'

      if (isFirst) { sMesh.add(this.boneMap['root']); isFirst = false }

      const parent = mesh.parent
      if (parent) { parent.remove(mesh); parent.add(sMesh) }

      // 必须在 add 到场景后、bind 之前更新 matrixWorld
      // 否则 bind() 使用的 bindMatrix (= matrixWorld) 会是错误的
      sMesh.updateMatrixWorld(true)
      sMesh.bind(this.skeleton)

      this.skinnedMeshEntries.push({ skinnedMesh: sMesh, originalMesh: mesh, parent })
    }

    console.log(`✅ 锚点骨骼绑定: ${allBones.length} 骨骼, ${this.skinnedMeshEntries.length} 蒙皮网格`)
  }

  /**
   * 自动检测头部区域
   * 策略: 利用顶点密度分析，找到头部和颈部之间的收窄区域（脖子）
   * 如果找不到脖子收窄点，降级为使用高度百分比方式
   */
  detectHeadRegion(model, bbox, size) {
    const totalHeight = bbox.max.y - bbox.min.y
    if (totalHeight <= 0) return null

    // 收集所有顶点
    const vertices = []
    model.traverse((child) => {
      if (!child.isMesh || !child.geometry) return
      const posArr = child.geometry.attributes.position.array
      const count = child.geometry.attributes.position.count
      // 使用世界矩阵将顶点变换到世界空间
      child.updateMatrixWorld(true)
      const meshWorldMat = child.matrixWorld.clone()

      for (let v = 0; v < count; v++) {
        const vtx = new THREE.Vector3(
          posArr[v * 3], posArr[v * 3 + 1], posArr[v * 3 + 2]
        ).applyMatrix4(meshWorldMat)
        vertices.push(vtx)
      }
    })

    if (vertices.length === 0) return null

    // 方法: 从模型顶部往下扫描，在每个高度切片计算 XZ 截面宽度
    // 找到"脖子"位置（宽度最小的收窄点），然后将脖子以上作为头部
    const sliceCount = 40 // 将顶部 35% 区域分成 40 个切片
    const scanTop = bbox.max.y
    const scanBottom = bbox.max.y - totalHeight * 0.35
    const sliceH = (scanTop - scanBottom) / sliceCount
    const sliceWidths = []

    for (let i = 0; i < sliceCount; i++) {
      const sliceBot = scanBottom + i * sliceH
      const sliceTop = sliceBot + sliceH
      let minX = Infinity, maxX = -Infinity
      let count = 0

      for (const v of vertices) {
        if (v.y >= sliceBot && v.y < sliceTop) {
          minX = Math.min(minX, v.x)
          maxX = Math.max(maxX, v.x)
          count++
        }
      }

      const width = count > 2 ? (maxX - minX) : 0
      sliceWidths.push({ y: (sliceBot + sliceTop) / 2, width, count, sliceBot, sliceTop })
    }

    // 找脖子收窄点: 从上往下（跳过最顶部几片，避免帽子边缘），找宽度最小的切片
    let neckSliceIdx = -1
    let minWidth = Infinity
    // 只在中间区域找脖子（跳过顶部 25% 和底部 25% 的切片）
    const searchStart = Math.floor(sliceCount * 0.2)
    const searchEnd = Math.floor(sliceCount * 0.75)

    for (let i = searchStart; i < searchEnd; i++) {
      if (sliceWidths[i].count > 5 && sliceWidths[i].width > 0) {
        if (sliceWidths[i].width < minWidth) {
          minWidth = sliceWidths[i].width
          neckSliceIdx = i
        }
      }
    }

    let headBottom, headTop
    if (neckSliceIdx >= 0) {
      // 找到脖子收窄点，头部 = 脖子上方到模型顶部
      headBottom = sliceWidths[neckSliceIdx].y
      headTop = bbox.max.y
      console.log(`🧠 脖子检测: Y=${headBottom.toFixed(3)}, 宽度=${minWidth.toFixed(4)}`)
    } else {
      // 降级：取顶部 18%
      headBottom = bbox.max.y - totalHeight * 0.18
      headTop = bbox.max.y
      console.log('🧠 脖子检测失败，降级为顶部 18%')
    }

    // 过渡区域：脖子下方再延伸一点
    const transitionExtend = totalHeight * 0.03
    const transitionBottom = headBottom - transitionExtend

    // 计算头部区域的 XZ 范围
    let headMinX = Infinity, headMaxX = -Infinity
    let headMinZ = Infinity, headMaxZ = -Infinity
    let headMinY = Infinity, headMaxY = -Infinity

    for (const v of vertices) {
      if (v.y >= headBottom) {
        headMinX = Math.min(headMinX, v.x)
        headMaxX = Math.max(headMaxX, v.x)
        headMinY = Math.min(headMinY, v.y)
        headMaxY = Math.max(headMaxY, v.y)
        headMinZ = Math.min(headMinZ, v.z)
        headMaxZ = Math.max(headMaxZ, v.z)
      }
    }

    if (headMinX === Infinity) {
      headMinX = bbox.min.x * 0.3
      headMaxX = bbox.max.x * 0.3
      headMinY = headBottom
      headMaxY = headTop
      headMinZ = bbox.min.z * 0.3
      headMaxZ = bbox.max.z * 0.3
    }

    // 稍微扩展
    const padX = (headMaxX - headMinX) * 0.05
    const padZ = (headMaxZ - headMinZ) * 0.05

    this.headBBox = new THREE.Box3(
      new THREE.Vector3(headMinX - padX, headMinY, headMinZ - padZ),
      new THREE.Vector3(headMaxX + padX, headMaxY, headMaxZ + padZ)
    )
    this.headSize = this.headBBox.getSize(new THREE.Vector3())
    // 过渡区域包围盒：在头部包围盒基础上外扩一圈
    const transitionPadAuto = transitionExtend
    this.headTransitionBBox = new THREE.Box3(
      new THREE.Vector3(headMinX - padX - transitionPadAuto, transitionBottom, headMinZ - padZ - transitionPadAuto),
      new THREE.Vector3(headMaxX + padX + transitionPadAuto, headMaxY + transitionPadAuto, headMaxZ + padZ + transitionPadAuto)
    )

    console.log(`🧠 头部区域: Y[${headMinY.toFixed(3)}, ${headMaxY.toFixed(3)}], ` +
                `高度=${this.headSize.y.toFixed(3)}, 宽度=${this.headSize.x.toFixed(3)}, ` +
                `过渡下限=${transitionBottom.toFixed(3)}`)

    return this.headBBox
  }

  /**
   * 相对坐标转头部区域局部坐标
   */
  relToHead(rel) {
    return new THREE.Vector3(
      this.headBBox.min.x + this.headSize.x * rel[0],
      this.headBBox.min.y + this.headSize.y * rel[1],
      this.headBBox.min.z + this.headSize.z * rel[2]
    )
  }

  /**
   * 判断顶点是否在头部区域内（3D 包围盒检查 + 过渡衰减）
   * 返回 0~1 的权重：
   *   1.0 = 完全在头部包围盒内
   *   0~1 = 过渡区域（基于到包围盒的距离衰减）
   *   0.0 = 完全在头部区域外
   * @param {THREE.Vector3} vtx 顶点世界坐标
   */
  headInfluence(vtx) {
    // 完全在头部包围盒内
    if (this.headBBox.containsPoint(vtx)) return 1.0

    // 检查是否在过渡区域内（外扩的包围盒）
    if (this.headTransitionBBox && !this.headTransitionBBox.containsPoint(vtx)) return 0.0

    // 计算到头部包围盒的距离进行过渡衰减
    const clamped = new THREE.Vector3().copy(vtx).clamp(this.headBBox.min, this.headBBox.max)
    const dist = vtx.distanceTo(clamped)
    const transSize = this.headTransitionBBox
      ? this.headBBox.min.distanceTo(this.headTransitionBBox.min)
      : this.headSize.y * 0.15
    if (dist >= transSize) return 0.0
    return 1.0 - (dist / transSize)
  }

  /**
   * 自动绑定骨骼到模型（仅头部区域）
   */
  autoBind(model, bbox, size) {
    this.destroy()
    this.modelBBox = bbox
    this.modelSize = size
    model.updateMatrixWorld(true)
    const invMat = model.matrixWorld.clone().invert()

    // 0. 自动检测头部区域
    const headDetected = this.detectHeadRegion(model, bbox, size)
    if (!headDetected) {
      console.error('❌ 头部区域检测失败')
      return
    }

    // 1. 创建骨骼（定位到头部区域）
    const allBones = []
    this.boneMap = {}
    this.boneRestPos = {}
    this.boneRestScale = {}

    for (const [id, rel] of BONE_DEFS) {
      const bone = new THREE.Bone()
      bone.name = id
      // 骨骼位置映射到头部区域而非全身
      const headPos = this.relToHead(rel)
      const localPos = headPos.clone()
      this.boneMap[id] = bone
      this.boneRestPos[id] = localPos.clone()
      this.boneRestScale[id] = new THREE.Vector3(1, 1, 1)
      allBones.push(bone)
    }

    // 2. 建立父子关系
    for (const [id, , parentId] of BONE_DEFS) {
      if (parentId && this.boneMap[parentId]) {
        this.boneMap[parentId].add(this.boneMap[id])
        const pPos = this.boneRestPos[parentId]
        const cPos = this.boneRestPos[id]
        this.boneMap[id].position.copy(cPos.clone().sub(pPos))
      } else if (!parentId) {
        this.boneMap[id].position.copy(this.boneRestPos[id])
      }
    }

    model.add(this.boneMap['root'])
    this.skeleton = new THREE.Skeleton(allBones)

    // 3. 收集需要转换的 Mesh
    const meshes = []
    model.traverse((child) => {
      if (child.isMesh && child.geometry) meshes.push(child)
    })

    // 4. 计算影响半径（基于头部区域骨骼间距）
    const bonePosArr = BONE_DEFS
      .filter(d => d[0] !== 'root')
      .map(d => this.relToHead(d[1]))
    let avgDist = 0, cnt = 0
    for (let a = 0; a < bonePosArr.length; a++) {
      for (let b = a + 1; b < bonePosArr.length; b++) {
        avgDist += bonePosArr[a].distanceTo(bonePosArr[b])
        cnt++
      }
    }
    avgDist = cnt > 0 ? avgDist / cnt : 0.1
    // 影响半径取骨骼间平均距离的 0.8 倍，防止影响过远
    const influenceR = Math.max(avgDist * 0.8, this.headSize.y * 0.3)

    console.log(`🦴 影响半径: ${influenceR.toFixed(4)}, 头部尺寸: ${this.headSize.y.toFixed(4)}`)

    // 5. 转换为 SkinnedMesh（只有头部区域顶点受骨骼影响）
    let isFirst = true
    this.skinnedMeshEntries = []

    for (const mesh of meshes) {
      const geo = mesh.geometry.clone()
      const vCount = geo.attributes.position.count
      const posArr = geo.attributes.position.array
      const skinIdx = new Float32Array(vCount * 4)
      const skinWgt = new Float32Array(vCount * 4)

      // 使用世界矩阵将顶点变换到世界空间
      mesh.updateMatrixWorld(true)
      const meshWorldMat = mesh.matrixWorld.clone()

      for (let v = 0; v < vCount; v++) {
        const vtx = new THREE.Vector3(
          posArr[v * 3], posArr[v * 3 + 1], posArr[v * 3 + 2]
        ).applyMatrix4(meshWorldMat)

        // 检查顶点是否在头部区域内
        const headWeight = this.headInfluence(vtx)

        if (headWeight < 0.001) {
          // 不在头部区域 → 绑定到 root 骨骼（不移动）
          skinIdx[v * 4] = 0 // root bone index
          skinIdx[v * 4 + 1] = 0
          skinIdx[v * 4 + 2] = 0
          skinIdx[v * 4 + 3] = 0
          skinWgt[v * 4] = 1
          skinWgt[v * 4 + 1] = 0
          skinWgt[v * 4 + 2] = 0
          skinWgt[v * 4 + 3] = 0
          continue
        }

        const dists = allBones.map((bone, idx) => {
          if (bone.name === 'root') return { idx, d: Infinity }
          return { idx, d: vtx.distanceTo(this.boneRestPos[bone.name]) }
        })
        dists.sort((a, b) => a.d - b.d)
        const top4 = dists.slice(0, 4)

        let weights = []
        let wSum = 0
        for (let k = 0; k < 4; k++) {
          if (k < top4.length && top4[k].d < influenceR) {
            const t = Math.max(0, 1 - top4[k].d / influenceR)
            const w = t * t * t * (t * (t * 6 - 15) + 10) // quintic smoothstep
            weights.push(w)
            wSum += w
          } else {
            weights.push(0)
          }
        }

        if (wSum > 0.001) {
          weights = weights.map(w => w / wSum)
          // 对过渡区域的顶点，混合 root 骨骼和面部骨骼的权重
          if (headWeight < 1.0) {
            // 按 headWeight 混合：部分面部骨骼 + 部分 root
            const rootWeight = 1.0 - headWeight
            for (let k = 0; k < 4; k++) {
              weights[k] *= headWeight
            }
            // 将剩余权重分配给 root
            // 找 root 在 top4 中的位置，或者替换最小权重
            let rootSlot = top4.findIndex(d => d.idx === 0)
            if (rootSlot === -1) {
              // root 不在 top4 中，替换最小权重的那个
              rootSlot = 3
              top4[3] = { idx: 0 }
            }
            weights[rootSlot] += rootWeight

            // 重新归一化
            wSum = weights.reduce((a, b) => a + b, 0)
            if (wSum > 0) weights = weights.map(w => w / wSum)
          }
        } else {
          weights = [1, 0, 0, 0]
          top4[0] = { idx: 0 }
        }

        for (let k = 0; k < 4; k++) {
          skinIdx[v * 4 + k] = k < top4.length ? top4[k].idx : 0
          skinWgt[v * 4 + k] = weights[k]
        }
      }

      geo.setAttribute('skinIndex', new THREE.Uint16BufferAttribute(skinIdx, 4))
      geo.setAttribute('skinWeight', new THREE.Float32BufferAttribute(skinWgt, 4))

      const sMat = mesh.material.clone()
      const sMesh = new THREE.SkinnedMesh(geo, sMat)
      sMesh.position.copy(mesh.position)
      sMesh.rotation.copy(mesh.rotation)
      sMesh.scale.copy(mesh.scale)
      sMesh.name = mesh.name + '_skinned'

      if (isFirst) { sMesh.add(this.boneMap['root']); isFirst = false }

      const parent = mesh.parent
      if (parent) { parent.remove(mesh); parent.add(sMesh) }

      // 必须在 add 到场景后、bind 之前更新 matrixWorld
      // 否则 bind() 使用的 bindMatrix (= matrixWorld) 会是错误的
      sMesh.updateMatrixWorld(true)
      sMesh.bind(this.skeleton)

      this.skinnedMeshEntries.push({ skinnedMesh: sMesh, originalMesh: mesh, parent })
    }

    console.log(`✅ 自动骨骼绑定(仅头部): ${allBones.length} 骨骼, ${this.skinnedMeshEntries.length} 蒙皮网格`)
  }

  /**
   * 构建形态键 (Shape Keys)
   * 为每个参数预计算顶点偏移量（仅头部区域）
   */
  buildShapeKeys() {
    this.shapeKeyDeltas.clear()
    if (!this.headBBox) return

    for (const paramDef of FACE_PARAMS_DEF) {
      const driverOps = DRIVERS[paramDef.id]
      if (!driverOps) continue
      const sw = SHAPE_WEIGHTS[paramDef.id] || 0.3
      const meshDeltas = new Map()

      for (const entry of this.skinnedMeshEntries) {
        const geo = entry.skinnedMesh.geometry
        const posArr = geo.attributes.position.array
        const vCount = geo.attributes.position.count
        const deltas = new Float32Array(vCount * 3)

        // 使用世界矩阵将顶点变换到世界空间（与骨骼位置一致）
        entry.skinnedMesh.updateMatrixWorld(true)
        const meshWorldMat = entry.skinnedMesh.matrixWorld.clone()
        // 世界矩阵的逆矩阵，用于将世界空间的 delta 转换回局部空间
        const meshWorldMatInv = meshWorldMat.clone().invert()
        // 提取纯旋转+缩放矩阵（去掉平移），用于变换方向向量
        const normalMatrix = new THREE.Matrix3().getNormalMatrix(meshWorldMat)
        const normalMatrixInv = new THREE.Matrix3().getNormalMatrix(meshWorldMatInv)

        // 形态键影响半径基于头部尺寸（缩小一点，避免影响整个头部）
        const radius = this.headSize.length() * 0.25

        for (let v = 0; v < vCount; v++) {
          const vtx = new THREE.Vector3(
            posArr[v * 3], posArr[v * 3 + 1], posArr[v * 3 + 2]
          ).applyMatrix4(meshWorldMat)

          // 检查是否在头部区域
          const headWeight = this.headInfluence(vtx)
          if (headWeight < 0.001) continue // 不在头部区域，delta 保持 0

          let dx = 0, dy = 0, dz = 0
          for (const [boneId, type, axis, factor] of driverOps) {
            if (type !== 'translate') continue
            const bp = this.boneRestPos[boneId]
            if (!bp) continue
            const dist = vtx.distanceTo(bp)
            if (dist >= radius) continue
            const t = Math.max(0, 1 - dist / radius)
            const influence = t * t * t * (t * (t * 6 - 15) + 10) * sw * headWeight
            
            // factor 现在是相对于头部尺寸的比例
            let absFactor = factor
            if (axis === 'x') absFactor *= this.headSize.x
            else if (axis === 'y') absFactor *= this.headSize.y
            else if (axis === 'z') absFactor *= this.headSize.z
            
            const delta = absFactor * influence
            if (axis === 'x') dx += delta
            else if (axis === 'y') dy += delta
            else dz += delta
          }

          // 将世界空间的 delta 转换回局部空间
          // delta 是方向向量（非位置），使用逆法线矩阵变换
          const worldDelta = new THREE.Vector3(dx, dy, dz)
          worldDelta.applyMatrix3(normalMatrixInv)

          deltas[v * 3] = worldDelta.x
          deltas[v * 3 + 1] = worldDelta.y
          deltas[v * 3 + 2] = worldDelta.z
        }
        meshDeltas.set(entry.skinnedMesh.uuid, deltas)
      }
      this.shapeKeyDeltas.set(paramDef.id, meshDeltas)
    }
    // 调试统计：检查形态键实际影响了多少顶点
    let totalAffected = 0
    let totalVerts = 0
    for (const [paramId, meshDeltas] of this.shapeKeyDeltas) {
      let paramAffected = 0
      for (const [, deltas] of meshDeltas) {
        const vCount = deltas.length / 3
        totalVerts += vCount
        for (let v = 0; v < vCount; v++) {
          const dx = deltas[v * 3], dy = deltas[v * 3 + 1], dz = deltas[v * 3 + 2]
          if (Math.abs(dx) > 0.0001 || Math.abs(dy) > 0.0001 || Math.abs(dz) > 0.0001) {
            paramAffected++
          }
        }
      }
      totalAffected += paramAffected
      if (paramAffected > 0) {
        console.log(`  📊 ${paramId}: ${paramAffected} 顶点受影响`)
      }
    }
    console.log(`✅ 形态键构建完成(头部区域): ${this.shapeKeyDeltas.size} 组, ` +
                `总受影响顶点: ${totalAffected}, 总顶点: ${totalVerts / this.shapeKeyDeltas.size}`)
  }

  /**
   * 应用驱动器 - 根据参数值同时驱动骨骼和形态键
   */
  applyDrivers(paramValues) {
    if (!this.skeleton) return

    // 1. 重置所有骨骼到初始位置/缩放/旋转
    for (const [id] of BONE_DEFS) {
      const bone = this.boneMap[id]
      if (!bone) continue
      if (id === 'root') {
        bone.position.copy(this.boneRestPos[id])
      } else {
        const parentId = BONE_DEFS.find(d => d[0] === id)?.[2]
        if (parentId) {
          const pPos = this.boneRestPos[parentId]
          const cPos = this.boneRestPos[id]
          bone.position.copy(cPos.clone().sub(pPos))
        }
      }
      bone.scale.set(1, 1, 1)
      bone.rotation.set(0, 0, 0)
    }

    // 2. 骨骼驱动
    for (const [paramId, value] of Object.entries(paramValues)) {
      if (Math.abs(value) < 0.001) continue
      const driverOps = DRIVERS[paramId]
      if (!driverOps) continue

      for (const [boneId, type, axis, factor] of driverOps) {
        const bone = this.boneMap[boneId]
        if (!bone) continue
        
        if (type === 'translate') {
          let absFactor = factor
          if (axis === 'x') absFactor *= this.headSize.x
          else if (axis === 'y') absFactor *= this.headSize.y
          else if (axis === 'z') absFactor *= this.headSize.z
          
          const delta = value * absFactor
          if (axis === 'x') bone.position.x += delta
          else if (axis === 'y') bone.position.y += delta
          else bone.position.z += delta
        } else if (type === 'scale') {
          const delta = value * factor
          const s = Math.max(0.2, 1 + delta) // 限制最小缩放防止反转
          if (axis === 'x') bone.scale.x *= s
          else if (axis === 'y') bone.scale.y *= s
          else bone.scale.z *= s
        } else if (type === 'rotate') {
          const delta = value * factor
          if (axis === 'x') bone.rotation.x += delta
          else if (axis === 'y') bone.rotation.y += delta
          else bone.rotation.z += delta
        }
      }
    }

    // 3. 形态键混合
    for (const entry of this.skinnedMeshEntries) {
      const geo = entry.skinnedMesh.geometry
      const posAttr = geo.attributes.position
      const origArr = entry.originalMesh.geometry.attributes.position.array

      // 从原始位置开始
      posAttr.array.set(origArr)

      // 叠加所有活跃的形态键
      for (const [paramId, value] of Object.entries(paramValues)) {
        if (Math.abs(value) < 0.001) continue
        const meshDeltas = this.shapeKeyDeltas.get(paramId)
        if (!meshDeltas) continue
        const deltas = meshDeltas.get(entry.skinnedMesh.uuid)
        if (!deltas) continue

        for (let i = 0; i < posAttr.array.length; i++) {
          posAttr.array[i] += deltas[i] * value
        }
      }

      posAttr.needsUpdate = true
      geo.computeVertexNormals()
    }
  }

  /**
   * 创建骨骼可视化辅助对象
   */
  createBoneHelpers(scene) {
    this.removeBoneHelpers(scene)
    this.boneHelperGroup = new THREE.Group()
    this.boneHelperGroup.name = 'boneHelpers'

    const colors = {
      face_center: 0xffffff,
      head_top: 0xcc88ff, head_left: 0xaa66dd, head_right: 0xaa66dd,
      jaw_left: 0xff6666, jaw_right: 0xff6666,
      cheek_left: 0xffaa44, cheek_right: 0xffaa44,
      chin: 0xff4444,
      eye_left: 0x44aaff, eye_right: 0x44aaff,
      nose_bridge: 0x44ff44, nose_tip: 0x44ff44,
      nose_left: 0x88ff88, nose_right: 0x88ff88,
      mouth_center: 0xff44ff,
      mouth_left: 0xff88ff, mouth_right: 0xff88ff,
      lip_upper: 0xffaaff, lip_lower: 0xffaaff,
      forehead_center: 0xffff44,
    }

    for (const [id] of BONE_DEFS) {
      if (id === 'root') continue
      const bone = this.boneMap[id]
      if (!bone) continue

      const color = colors[id] || 0xffffff
      // 球体大小根据头部尺寸动态调整
      const sphereR = this.headSize ? this.headSize.y * 0.02 : 0.005
      const sphereGeo = new THREE.SphereGeometry(sphereR, 8, 8)
      const sphereMat = new THREE.MeshBasicMaterial({
        color, transparent: true, opacity: 0.8, depthTest: false
      })
      const sphere = new THREE.Mesh(sphereGeo, sphereMat)

      // 获取骨骼世界位置
      const wp = new THREE.Vector3()
      bone.getWorldPosition(wp)
      sphere.position.copy(wp)
      sphere.renderOrder = 999
      this.boneHelperGroup.add(sphere)
    }

    scene.add(this.boneHelperGroup)
  }

  removeBoneHelpers(scene) {
    if (this.boneHelperGroup) {
      scene.remove(this.boneHelperGroup)
      this.boneHelperGroup.traverse((child) => {
        if (child.geometry) child.geometry.dispose()
        if (child.material) child.material.dispose()
      })
      this.boneHelperGroup = null
    }
  }

  updateBoneHelpers() {
    if (!this.boneHelperGroup) return
    let idx = 0
    for (const [id] of BONE_DEFS) {
      if (id === 'root') continue
      const bone = this.boneMap[id]
      if (!bone) continue
      const sphere = this.boneHelperGroup.children[idx]
      if (sphere) {
        const wp = new THREE.Vector3()
        bone.getWorldPosition(wp)
        sphere.position.copy(wp)
      }
      idx++
    }
  }

  /**
   * 烘焙当前变形结果到顶点数组中
   * 将形态键 + 骨骼变换的最终结果写入顶点位置
   * 返回每个 mesh 的烘焙后顶点数据
   */
  bakeDeformed() {
    if (!this.skeleton || this.skinnedMeshEntries.length === 0) {
      console.warn('bakeDeformed: 骨骼系统未初始化')
      return null
    }

    // 更新骨骼矩阵
    this.skeleton.update()

    const results = []

    for (const entry of this.skinnedMeshEntries) {
      const sMesh = entry.skinnedMesh
      const geo = sMesh.geometry
      const posAttr = geo.attributes.position
      const skinIdxAttr = geo.attributes.skinIndex
      const skinWgtAttr = geo.attributes.skinWeight
      const vCount = posAttr.count

      if (!skinIdxAttr || !skinWgtAttr) {
        // 没有蒙皮数据，直接用当前顶点（已包含形态键变形）
        results.push({
          mesh: sMesh,
          positions: new Float32Array(posAttr.array),
          normals: geo.attributes.normal ? new Float32Array(geo.attributes.normal.array) : null,
          uvs: geo.attributes.uv ? new Float32Array(geo.attributes.uv.array) : null,
          indices: geo.index ? new Uint32Array(geo.index.array) : null,
        })
        continue
      }

      // 获取骨骼逆绑定矩阵和骨骼世界矩阵
      const bones = this.skeleton.bones
      const boneInverses = this.skeleton.boneInverses
      const boneCount = bones.length

      // 计算每个骨骼的最终变换矩阵 = bone.matrixWorld * boneInverse
      // 但因为 SkinnedMesh 的 bind matrix 也需要考虑
      sMesh.updateMatrixWorld(true)
      const bindMatrix = sMesh.bindMatrix
      const bindMatrixInverse = sMesh.bindMatrixInverse

      const boneMatrices = []
      for (let b = 0; b < boneCount; b++) {
        const mat = new THREE.Matrix4()
        mat.multiplyMatrices(bones[b].matrixWorld, boneInverses[b])
        boneMatrices.push(mat)
      }

      // 烘焙每个顶点
      const bakedPos = new Float32Array(vCount * 3)

      for (let v = 0; v < vCount; v++) {
        // 当前顶点位置（已包含形态键变形）
        const srcX = posAttr.array[v * 3]
        const srcY = posAttr.array[v * 3 + 1]
        const srcZ = posAttr.array[v * 3 + 2]

        // 应用 bindMatrix 将顶点转到骨骼空间
        const skinVertex = new THREE.Vector4(srcX, srcY, srcZ, 1)
        skinVertex.applyMatrix4(bindMatrix)

        // 蒙皮混合
        let finalX = 0, finalY = 0, finalZ = 0

        for (let k = 0; k < 4; k++) {
          const boneIdx = skinIdxAttr.array[v * 4 + k]
          const weight = skinWgtAttr.array[v * 4 + k]
          if (weight < 0.0001) continue

          const boneMat = boneMatrices[boneIdx]
          if (!boneMat) continue

          // 应用骨骼矩阵
          const transformed = new THREE.Vector4(
            skinVertex.x, skinVertex.y, skinVertex.z, 1
          ).applyMatrix4(boneMat)

          finalX += transformed.x * weight
          finalY += transformed.y * weight
          finalZ += transformed.z * weight
        }

        // 应用 bindMatrixInverse 转回模型空间
        const result = new THREE.Vector4(finalX, finalY, finalZ, 1)
        result.applyMatrix4(bindMatrixInverse)

        bakedPos[v * 3] = result.x
        bakedPos[v * 3 + 1] = result.y
        bakedPos[v * 3 + 2] = result.z
      }

      // 重新计算法线
      const bakedGeo = new THREE.BufferGeometry()
      bakedGeo.setAttribute('position', new THREE.BufferAttribute(bakedPos, 3))
      if (geo.attributes.uv) {
        bakedGeo.setAttribute('uv', new THREE.BufferAttribute(
          new Float32Array(geo.attributes.uv.array), 2
        ))
      }
      if (geo.index) {
        bakedGeo.setIndex(new THREE.BufferAttribute(new Uint32Array(geo.index.array), 1))
      }
      bakedGeo.computeVertexNormals()

      results.push({
        mesh: sMesh,
        positions: bakedPos,
        normals: bakedGeo.attributes.normal ? new Float32Array(bakedGeo.attributes.normal.array) : null,
        uvs: geo.attributes.uv ? new Float32Array(geo.attributes.uv.array) : null,
        indices: geo.index ? new Uint32Array(geo.index.array) : null,
      })

      bakedGeo.dispose()
    }

    return results
  }

  /**
   * 销毁骨骼系统
   */
  destroy() {
    for (const entry of this.skinnedMeshEntries) {
      if (entry.skinnedMesh && entry.parent) {
        entry.parent.remove(entry.skinnedMesh)
        entry.parent.add(entry.originalMesh)
        const origPosArr = entry.originalMesh.geometry.attributes.position
        origPosArr.needsUpdate = true
        entry.originalMesh.geometry.computeVertexNormals()
      }
    }

    if (this.boneMap['root'] && this.boneMap['root'].parent) {
      this.boneMap['root'].parent.remove(this.boneMap['root'])
    }

    this.skeleton = null
    this.boneMap = {}
    this.boneRestPos = {}
    this.boneRestScale = {}
    this.skinnedMeshEntries = []
    this.shapeKeyDeltas.clear()
  }
}