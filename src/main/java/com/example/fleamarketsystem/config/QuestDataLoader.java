package com.example.fleamarketsystem.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.fleamarketsystem.entity.Quest;
import com.example.fleamarketsystem.repository.QuestRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuestDataLoader implements CommandLineRunner {

    private final QuestRepository questRepository;
    
    // ★修正: ここで直接インスタンス化します
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(String... args) throws Exception {
        loadQuestData();
    }

    private void loadQuestData() {
        String jsonPath = "/data/quests.json";

        try (InputStream inputStream = TypeReference.class.getResourceAsStream(jsonPath)) {
            if (inputStream == null) {
                System.out.println("クエストデータが見つかりません: " + jsonPath);
                return;
            }

            List<Quest> quests = objectMapper.readValue(inputStream, new TypeReference<List<Quest>>() {});

            for (Quest jsonQuest : quests) {
                // DBから既存のクエストを検索
                Quest existingQuest = questRepository.findByConditionCode(jsonQuest.getConditionCode())
                        .orElse(null);

                if (existingQuest == null) {
                    // --- 新規登録 ---
                    questRepository.save(jsonQuest);
                    System.out.println("新規クエストを登録しました: " + jsonQuest.getTitle());
                } else {
                    // --- ★追加: 既存クエストの更新処理 ---
                    // JSONの内容でDBの値を上書きします
                    existingQuest.setTitle(jsonQuest.getTitle());
                    existingQuest.setDescription(jsonQuest.getDescription());
                    existingQuest.setQuestType(jsonQuest.getQuestType());
                    existingQuest.setRewardAmount(jsonQuest.getRewardAmount());
                    
                    questRepository.save(existingQuest);
                    System.out.println("クエスト情報を更新しました: " + jsonQuest.getTitle());
                }
            }
            
        } catch (IOException e) {
            System.err.println("クエストデータの読み込みに失敗しました: " + e.getMessage());
            e.printStackTrace();
        }
    }
}