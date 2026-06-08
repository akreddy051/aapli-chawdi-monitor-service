package com.example.aapliChawdi.client;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class GeminiClient {

    private String callGemini(String prompt) {
        try (Client client = new Client()) {
            Content content = Content.builder()
                    .role("user")
                    .parts(Arrays.asList(Part.fromText(prompt)))
                    .build();
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash",
                    content,
                    null
            );
            return response.text();
        } catch (Exception e) {
            throw new RuntimeException("Error while calling Gemini API", e);
        }
    }

    private String analyzeImage(String imagePathString, String promptText) {
        try (Client client = new Client()) {
            Path path = Paths.get(imagePathString);
            byte[] imageBytes = Files.readAllBytes(path);
            String mimeType = Files.probeContentType(path);
            Part imagePart = Part.fromBytes(imageBytes, mimeType);
            Part textPart = Part.fromText(promptText);

            Content multimodalContent = Content.builder()
                    .role("user")
                    .parts(Arrays.asList(textPart, imagePart))
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash",
                    multimodalContent,
                    null
            );

            return response.text();

        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new RuntimeException(
                    "Failed to read image file: " + imagePathString,
                    e
            );
        } catch (Exception e) {
            log.debug(e.getMessage());
            throw new RuntimeException(
                    "Error while calling Gemini Vision API",
                    e
            );
        }
    }

    public String solveCaptcha(String imagePath) {
        return analyzeImage(
                imagePath,
                "Extract all digits from the image. Return only the raw numbers."
        );
    }

    public String validateDistrict(String input) {
        String prompt = """
                You are a Maharashtra land records assistant.
                The user entered this as a Maharashtra district name: "%s"
                
                Rules:
                - Accept English, Marathi, transliterations, minor spelling mistakes
                - If valid, return ONLY the correct Marathi district name as used on Maharashtra government portals
                - If invalid or unrecognizable as any Maharashtra district, return: INVALID
                
                Return only the Marathi name or INVALID. Nothing else.
                """.formatted(input);
        return callGemini(prompt);
    }

    public String validateTaluka(String input, String district) {
        String prompt = """
                You are a Maharashtra land records assistant.
                The user entered this as a taluka name: "%s"
                It should belong to this district in Maharashtra: "%s"
                
                Rules:
                - Accept English, Marathi, transliterations, minor spelling mistakes
                - If this is a valid taluka under that district, return ONLY the correct Marathi taluka name
                - If invalid or doesn't belong to that district, return: INVALID
                
                Return only the Marathi name or INVALID. Nothing else.
                """.formatted(input, district);
        return callGemini(prompt);
    }

    public String validateVillage(String input, String taluka, String district) {
        String prompt = """
                You are a Maharashtra land records assistant.
                The user entered this as a village name: "%s"
                It should belong to taluka "%s" under district "%s" in Maharashtra.
                
                Rules:
                - Accept English, Marathi, transliterations, minor spelling mistakes
                - If this is a valid village under that taluka and district, return ONLY the correct Marathi village name
                - If invalid or unrecognizable, return: INVALID
                
                Return only the Marathi name or INVALID. Nothing else.
                """.formatted(input, taluka, district);
        return callGemini(prompt);
    }

    public String summarizeNotice(String noticeText) {
        String prompt =
                "You are analyzing a Maharashtra land mutation notice written in Marathi.\n\n" +

                        "IMPORTANT:\n" +
                        "First determine whether the provided text is a genuine land mutation notice.\n\n" +

                        "If the text contains an error page, technical error, network issue, empty content, report generation failure, captcha failure, unrelated content, or insufficient information to identify a mutation notice, return EXACTLY:\n\n" +
                        "DATA_NOT_FOUND\n\n" +

                        "Do not return any explanation.\n" +
                        "Do not return markdown.\n" +
                        "Do not return a summary.\n\n" +

                        "If the content is a genuine mutation notice, summarize it in concise English.\n\n" +

                        "Keep the summary under 100 words.\n\n" +

                        "Identify the nature of the mutation such as sale, inheritance, succession, gift, partition, court order, correction, government action, mortgage release, or any other mutation type.\n\n" +

                        "Extract all important details that are actually present in the notice.\n\n" +

                        "Format as:\n\n" +

                        "📄 Summary\n" +
                        "<short summary>\n\n" +

                        "🏷 Mutation Type\n" +
                        "<identified mutation type>\n\n" +

                        "👥 Parties Involved\n" +
                        "<list only the parties mentioned>\n\n" +

                        "🌾 Land Details\n" +
                        "<survey numbers, area, village and other relevant land information>\n\n" +

                        "💰 Financial Details\n" +
                        "<transaction amount or financial information if present>\n\n" +

                        "📅 Important Dates\n" +
                        "<all important dates found>\n\n" +

                        "⚠️ Action Required\n" +
                        "<brief action or impact for affected landowners>\n\n" +

                        "Only include fields that are actually present in the notice.\n" +
                        "Do not invent values.\n" +
                        "Do not write 'Not Mentioned' repeatedly.\n\n" +

                        "Notice:\n\n" +
                        noticeText;

        return callGemini(prompt);
    }

    public String matchFromList(String input, List<String> options) {
        String optionsList = String.join("\n", options);
        String prompt = """
            You are a strict data matcher.
            
            The user entered: "%s"
            
            You MUST match it to an entry from this list and ONLY this list:
            %s
            
            Rules:
            - Your answer MUST be one of the entries from the list above, word for word
            - Handle minor spelling mistakes, English transliterations, and Marathi variations
            - If the input is a reasonable match for an entry in the list, return that exact entry
            - If nothing in the list is a reasonable match, return: INVALID
            - Do NOT use your own knowledge
            - Do NOT return anything that is not in the list above
            - Do NOT correct or modify any entry from the list
            
            Return only the exact entry from the list or INVALID. Nothing else.
            """.formatted(input, optionsList);
        return callGemini(prompt);
    }
}