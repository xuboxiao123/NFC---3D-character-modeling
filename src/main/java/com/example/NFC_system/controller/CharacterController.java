package com.example.NFC_system.controller;

import java.io.File;

import com.example.NFC_system.entity.Character;
import com.example.NFC_system.entity.CharacterVersion;
import com.example.NFC_system.entity.CommunityShare;
import com.example.NFC_system.repository.CharacterRepository;
import com.example.NFC_system.repository.CharacterVersionRepository;
import com.example.NFC_system.repository.CommunityShareRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import com.example.NFC_system.service.DoubaoService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.example.NFC_system.service.HunyuanService;

@RestController
@RequestMapping("/api/character")
public class CharacterController {

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private CharacterVersionRepository versionRepository;

    @Autowired
    private CommunityShareRepository shareRepository;

    @PostMapping("/create")
    public Character createCharacter(@RequestParam String name) {

        Character character = new Character();
        character.setName(name);
        character.setUserId(1L);
        character.setCreatedAt(LocalDateTime.now());

        return characterRepository.save(character);
    }

     @Autowired
    private DoubaoService doubaoService;

   

    @Autowired
    private HunyuanService hunyuanService;
    @PostMapping("/generate-3d")
    public Map<String, String> generate3D(@RequestBody Map<String, String> payload) throws Exception {

    String imageUrl = payload.get("imageUrl");

    String jobId = hunyuanService.submitTask(imageUrl);

    Map<String, String> result = new HashMap<>();
    result.put("jobId", jobId);

    return result;
    }

    @PostMapping("/query-3d")
    public String query3D(@RequestBody Map<String, String> request) throws Exception {
    String jobId = request.get("jobId");
    return hunyuanService.queryTask(jobId);
    }

    @GetMapping("/list")
    public List<Character> list(@RequestParam Long userId) {
        return characterRepository.findByUserIdOrderBySortOrderAsc(userId);
    }

    // 删除角色（级联删除其所有版本和社区分享）
    @DeleteMapping("/delete")
    @Transactional
    public Map<String, Object> deleteCharacter(@RequestParam Long characterId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 先删除该角色下所有版本关联的社区分享
            List<CharacterVersion> versions = versionRepository.findByCharacterId(characterId);
            for (CharacterVersion v : versions) {
                Optional<CommunityShare> share = shareRepository.findByVersionId(v.getId());
                share.ifPresent(shareRepository::delete);
            }
            // 删除所有版本
            versionRepository.deleteByCharacterId(characterId);
            // 删除角色
            characterRepository.deleteById(characterId);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 批量更新排序
    @PutMapping("/reorder")
    @Transactional
    public Map<String, Object> reorder(@RequestBody List<Map<String, Object>> orderList) {
        Map<String, Object> result = new HashMap<>();
        try {
            for (Map<String, Object> item : orderList) {
                Long id = Long.valueOf(item.get("id").toString());
                Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
                characterRepository.findById(id).ifPresent(c -> {
                    c.setSortOrder(sortOrder);
                    characterRepository.save(c);
                });
            }
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }


    
}
