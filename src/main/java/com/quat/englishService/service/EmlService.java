package com.quat.englishService.service;

import com.quat.englishService.service.CollocationHistoryService.CollocationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmlService {

    private static final Logger logger = LoggerFactory.getLogger(EmlService.class);
    private final CollocationHistoryService collocationHistoryService;

    public EmlService(CollocationHistoryService collocationHistoryService) {
        this.collocationHistoryService = collocationHistoryService;
    }

    /**
     * Create an EML email file containing a table with all collocations from history
     */
    public String createCollocationHistoryEml() {
        try {
            // Get all collocations from history
            List<CollocationEntry> allCollocations = collocationHistoryService.getAllCollocationHistory();
            
            if (allCollocations.isEmpty()) {
                logger.warn("No collocations found in history, creating empty EML");
            }

            // Generate EML content
            String emlContent = generateEmlContent(allCollocations);
            
            // Save to file
            String fileName = "collocation_history_" + 
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm")) + ".eml";
            Path emlPath = Paths.get(fileName);
            
            Files.writeString(emlPath, emlContent);
            logger.info("Created EML file with {} collocations: {}", allCollocations.size(), fileName);
            
            return emlPath.toAbsolutePath().toString();

        } catch (Exception e) {
            logger.error("Error creating collocation history EML: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create collocation history EML", e);
        }
    }

    private String generateEmlContent(List<CollocationEntry> collocations) {
        StringBuilder eml = new StringBuilder();
        
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss +0000"));
        
        // EML headers
        eml.append("Date: ").append(currentDate).append("\r\n");
        eml.append("From: TOEIC Learning System <toeic@learning.system>\r\n");
        eml.append("To: Student <student@example.com>\r\n");
        eml.append("Subject: Complete TOEIC Collocations History - ").append(collocations.size()).append(" Items\r\n");
        eml.append("MIME-Version: 1.0\r\n");
        eml.append("Content-Type: text/html; charset=UTF-8\r\n");
        eml.append("Content-Transfer-Encoding: 8bit\r\n");
        eml.append("\r\n");

        // HTML content
        eml.append(generateHtmlTable(collocations));

        return eml.toString();
    }

    private String generateHtmlTable(List<CollocationEntry> collocations) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"en\">\n")
            .append("<head>\n")
            .append("    <meta charset=\"UTF-8\">\n")
            .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            .append("    <title>Complete TOEIC Collocations History</title>\n")
            .append("    <style>\n")
            .append("        body {\n")
            .append("            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n")
            .append("            margin: 20px;\n")
            .append("            background-color: #f5f7fa;\n")
            .append("            color: #2c3e50;\n")
            .append("        }\n")
            .append("        .container {\n")
            .append("            max-width: 1200px;\n")
            .append("            margin: 0 auto;\n")
            .append("            background-color: white;\n")
            .append("            padding: 30px;\n")
            .append("            border-radius: 10px;\n")
            .append("            box-shadow: 0 4px 12px rgba(0,0,0,0.1);\n")
            .append("        }\n")
            .append("        .header {\n")
            .append("            text-align: center;\n")
            .append("            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n")
            .append("            color: white;\n")
            .append("            padding: 30px;\n")
            .append("            border-radius: 10px;\n")
            .append("            margin-bottom: 30px;\n")
            .append("        }\n")
            .append("        .header h1 {\n")
            .append("            margin: 0;\n")
            .append("            font-size: 2.2em;\n")
            .append("        }\n")
            .append("        .header p {\n")
            .append("            margin: 10px 0 0 0;\n")
            .append("            font-size: 1.1em;\n")
            .append("            opacity: 0.9;\n")
            .append("        }\n")
            .append("        .stats {\n")
            .append("            display: flex;\n")
            .append("            justify-content: center;\n")
            .append("            gap: 30px;\n")
            .append("            margin-bottom: 30px;\n")
            .append("            flex-wrap: wrap;\n")
            .append("        }\n")
            .append("        .stat-card {\n")
            .append("            background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%);\n")
            .append("            color: white;\n")
            .append("            padding: 20px;\n")
            .append("            border-radius: 8px;\n")
            .append("            text-align: center;\n")
            .append("            min-width: 120px;\n")
            .append("        }\n")
            .append("        .stat-number {\n")
            .append("            font-size: 2em;\n")
            .append("            font-weight: bold;\n")
            .append("            margin-bottom: 5px;\n")
            .append("        }\n")
            .append("        .stat-label {\n")
            .append("            font-size: 0.9em;\n")
            .append("            opacity: 0.9;\n")
            .append("        }\n")
            .append("        table {\n")
            .append("            width: 100%;\n")
            .append("            border-collapse: collapse;\n")
            .append("            margin-top: 20px;\n")
            .append("            font-size: 14px;\n")
            .append("        }\n")
            .append("        th {\n")
            .append("            background: linear-gradient(135deg, #2d3436 0%, #636e72 100%);\n")
            .append("            color: white;\n")
            .append("            padding: 15px 12px;\n")
            .append("            text-align: left;\n")
            .append("            font-weight: 600;\n")
            .append("            font-size: 13px;\n")
            .append("            text-transform: uppercase;\n")
            .append("            letter-spacing: 0.5px;\n")
            .append("        }\n")
            .append("        td {\n")
            .append("            padding: 12px;\n")
            .append("            border-bottom: 1px solid #e9ecef;\n")
            .append("            vertical-align: top;\n")
            .append("        }\n")
            .append("        tr:nth-child(even) {\n")
            .append("            background-color: #f8f9fa;\n")
            .append("        }\n")
            .append("        tr:hover {\n")
            .append("            background-color: #e3f2fd;\n")
            .append("            transition: background-color 0.2s ease;\n")
            .append("        }\n")
            .append("        .collocation {\n")
            .append("            font-weight: bold;\n")
            .append("            color: #2c5aa0;\n")
            .append("            font-size: 15px;\n")
            .append("        }\n")
            .append("        .ipa {\n")
            .append("            font-style: italic;\n")
            .append("            color: #6c757d;\n")
            .append("            font-size: 13px;\n")
            .append("        }\n")
            .append("        .meaning {\n")
            .append("            color: #495057;\n")
            .append("            line-height: 1.4;\n")
            .append("        }\n")
            .append("        .example {\n")
            .append("            font-style: italic;\n")
            .append("            color: #28a745;\n")
            .append("            line-height: 1.4;\n")
            .append("            background: #f8fff9;\n")
            .append("            padding: 8px;\n")
            .append("            border-radius: 4px;\n")
            .append("            border-left: 3px solid #28a745;\n")
            .append("        }\n")
            .append("        .vietnamese {\n")
            .append("            color: #dc3545;\n")
            .append("            font-size: 13px;\n")
            .append("            background: #fff5f5;\n")
            .append("            padding: 6px;\n")
            .append("            border-radius: 4px;\n")
            .append("            border-left: 3px solid #dc3545;\n")
            .append("            line-height: 1.3;\n")
            .append("        }\n")
            .append("        .date {\n")
            .append("            font-size: 12px;\n")
            .append("            color: #6c757d;\n")
            .append("            background: #e9ecef;\n")
            .append("            padding: 4px 8px;\n")
            .append("            border-radius: 3px;\n")
            .append("            font-family: monospace;\n")
            .append("        }\n")
            .append("        .footer {\n")
            .append("            text-align: center;\n")
            .append("            margin-top: 40px;\n")
            .append("            padding: 20px;\n")
            .append("            background: #f8f9fa;\n")
            .append("            border-radius: 8px;\n")
            .append("            color: #6c757d;\n")
            .append("        }\n")
            .append("        @media (max-width: 768px) {\n")
            .append("            .container { padding: 15px; }\n")
            .append("            table { font-size: 12px; }\n")
            .append("            th, td { padding: 8px 6px; }\n")
            .append("            .stats { flex-direction: column; align-items: center; }\n")
            .append("        }\n")
            .append("    </style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("    <div class=\"container\">\n")
            .append("        <div class=\"header\">\n")
            .append("            <h1>📚 Complete TOEIC Collocations History</h1>\n")
            .append("            <p>Your comprehensive learning reference - all collocations in one place</p>\n")
            .append("        </div>\n");

        // Add statistics
        long totalCollocations = collocations.size();
        long uniqueDates = collocations.stream().map(CollocationEntry::getDate).distinct().count();
        String earliestDate = collocations.stream()
                .map(CollocationEntry::getDate)
                .min(String::compareTo)
                .orElse("N/A");
        String latestDate = collocations.stream()
                .map(CollocationEntry::getDate)
                .max(String::compareTo)
                .orElse("N/A");

        html.append("        <div class=\"stats\">\n")
            .append("            <div class=\"stat-card\">\n")
            .append("                <div class=\"stat-number\">").append(totalCollocations).append("</div>\n")
            .append("                <div class=\"stat-label\">Total Collocations</div>\n")
            .append("            </div>\n")
            .append("            <div class=\"stat-card\">\n")
            .append("                <div class=\"stat-number\">").append(uniqueDates).append("</div>\n")
            .append("                <div class=\"stat-label\">Study Days</div>\n")
            .append("            </div>\n")
            .append("            <div class=\"stat-card\">\n")
            .append("                <div class=\"stat-number\">").append(earliestDate).append("</div>\n")
            .append("                <div class=\"stat-label\">First Entry</div>\n")
            .append("            </div>\n")
            .append("            <div class=\"stat-card\">\n")
            .append("                <div class=\"stat-number\">").append(latestDate).append("</div>\n")
            .append("                <div class=\"stat-label\">Latest Entry</div>\n")
            .append("            </div>\n")
            .append("        </div>\n");

        // Add table
        html.append("        <table>\n")
            .append("            <thead>\n")
            .append("                <tr>\n")
            .append("                    <th width=\"5%\">#</th>\n")
            .append("                    <th width=\"8%\">Date</th>\n")
            .append("                    <th width=\"12%\">Collocation</th>\n")
            .append("                    <th width=\"10%\">IPA</th>\n")
            .append("                    <th width=\"18%\">Meaning</th>\n")
            .append("                    <th width=\"22%\">Example</th>\n")
            .append("                    <th width=\"25%\">Vietnamese</th>\n")
            .append("                </tr>\n")
            .append("            </thead>\n")
            .append("            <tbody>\n");

        for (int i = 0; i < collocations.size(); i++) {
            CollocationEntry entry = collocations.get(i);
            html.append("                <tr>\n")
                .append("                    <td><strong>").append(i + 1).append("</strong></td>\n")
                .append("                    <td><span class=\"date\">").append(escapeHtml(entry.getDate())).append("</span></td>\n")
                .append("                    <td><span class=\"collocation\">").append(escapeHtml(entry.getCollocation())).append("</span></td>\n")
                .append("                    <td><span class=\"ipa\">").append(escapeHtml(entry.getIpa())).append("</span></td>\n")
                .append("                    <td><span class=\"meaning\">").append(escapeHtml(entry.getMeaning())).append("</span></td>\n")
                .append("                    <td><div class=\"example\">").append(escapeHtml(entry.getExample())).append("</div></td>\n")
                .append("                    <td><div class=\"vietnamese\">").append(escapeHtml(entry.getVietnamese())).append("</div></td>\n")
                .append("                </tr>\n");
        }

        html.append("            </tbody>\n")
            .append("        </table>\n")
            .append("        <div class=\"footer\">\n")
            .append("            <p><strong>Generated:</strong> ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm"))).append("</p>\n")
            .append("            <p><em>This is your complete TOEIC collocations learning history. Use it as a reference for review and practice.</em></p>\n")
            .append("        </div>\n")
            .append("    </div>\n")
            .append("</body>\n")
            .append("</html>");

        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
