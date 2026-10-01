package com.robosvrsbc;

import com.google.gson.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final String EVENT_ID = "13Kdkn7Kj6Kg2TwdSoMFGU1kN@PE7xTOfzqyW03Dsno=";
    private static final String BASE_URL = "https://whova.com/xems/apis/event_webpage/agenda/public/";
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        try {
            JsonArray resultJson = crawlAgenda();
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            String jsonOutput = gson.toJson(resultJson);

            // Imprime no console/terminal
            System.out.println(jsonOutput);

            // Salva automaticamente no arquivo resultado.json no diretório raiz do projeto
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of("resultado.json"),
                    jsonOutput,
                    StandardCharsets.UTF_8
            );
            System.err.println("\n[SUCESSO] O resultado JSON foi salvo no arquivo: resultado.json");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            EXECUTOR.shutdown();
        }
    }

    public static JsonArray crawlAgenda() throws Exception {
        JsonArray outputArray = new JsonArray();

        String encodedEventId = URLEncoder.encode(EVENT_ID, StandardCharsets.UTF_8);
        String agendasUrl = BASE_URL + "get_agendas?event_id=" + encodedEventId;

        String rawJson = fetchHttpResponse(agendasUrl);
        JsonObject rootObj = JsonParser.parseString(rawJson).getAsJsonObject();

        JsonObject dataObj = rootObj.getAsJsonObject("data");
        if (dataObj == null || !dataObj.has("agenda")) {
            return outputArray;
        }

        JsonArray agendaDays = dataObj.getAsJsonArray("agenda");

        // Locate Oct 02, 2026
        JsonObject oct02Day = null;
        for (JsonElement dayElem : agendaDays) {
            JsonObject dayObj = dayElem.getAsJsonObject();
            String dateStr = dayObj.has("date") ? dayObj.get("date").getAsString() : "";
            if (dateStr.contains("Oct 02") || dateStr.contains("Oct 2") || dateStr.contains("02/10")) {
                oct02Day = dayObj;
                break;
            }
        }

        if (oct02Day == null) {
            return outputArray;
        }

        JsonArray timeRanges = oct02Day.getAsJsonArray("time_ranges");
        if (timeRanges == null) {
            return outputArray;
        }

        List<CompletableFuture<JsonObject>> sessionFutures = new ArrayList<>();

        for (JsonElement trElem : timeRanges) {
            JsonArray trArray = trElem.getAsJsonArray();
            if (trArray.size() < 2) continue;

            JsonArray locationGroups = trArray.get(1).getAsJsonArray();
            for (JsonElement lgElem : locationGroups) {
                JsonArray locArray = lgElem.getAsJsonArray();
                for (JsonElement subElem : locArray) {
                    JsonObject locObj = subElem.getAsJsonObject();
                    if (!locObj.has("sessions")) continue;

                    JsonArray sessions = locObj.getAsJsonArray("sessions");
                    for (JsonElement sessElem : sessions) {
                        JsonObject sess = sessElem.getAsJsonObject();
                        String name = sess.has("name") ? sess.get("name").getAsString() : "";
                        String desc = sess.has("desc") ? sess.get("desc").getAsString() : "";
                        String place = sess.has("place") ? sess.get("place").getAsString() : "";
                        String startTime = sess.has("start_time") ? sess.get("start_time").getAsString() : "";
                        String endTime = sess.has("end_time") ? sess.get("end_time").getAsString() : "";
                        String sessionId = sess.has("id") ? sess.get("id").getAsString() : "";

                        List<String> trackNames = new ArrayList<>();
                        if (sess.has("tracks")) {
                            for (JsonElement tElem : sess.getAsJsonArray("tracks")) {
                                if (tElem.isJsonObject()) {
                                    JsonObject tObj = tElem.getAsJsonObject();
                                    if (tObj.has("name")) {
                                        trackNames.add(tObj.get("name").getAsString());
                                    }
                                }
                            }
                        }

                        // Check if session has SBGAMES marker
                        boolean isSbgames = trackNames.stream().anyMatch(t -> t.equalsIgnoreCase("SBGames") || t.equalsIgnoreCase("SBGAMES"))
                                || name.toUpperCase().contains("SBGAMES")
                                || desc.toUpperCase().contains("SBGAMES");

                        if (!isSbgames) {
                            continue;
                        }

                        sessionFutures.add(CompletableFuture.supplyAsync(() -> {
                            try {
                                String sessionDetailUrl = BASE_URL + "get_session_page_data?event_id=" + encodedEventId + "&session_id=" + sessionId;
                                String sessionDetailJsonStr = fetchHttpResponse(sessionDetailUrl);
                                JsonObject sessionDetailRoot = JsonParser.parseString(sessionDetailJsonStr).getAsJsonObject();

                                JsonObject sessionDetailData = sessionDetailRoot.getAsJsonObject("data");
                                if (sessionDetailData == null || !sessionDetailData.has("s")) {
                                    return null;
                                }

                                JsonObject sObj = sessionDetailData.getAsJsonObject("s");
                                JsonArray programs = sObj.has("programs") ? sObj.getAsJsonArray("programs") : new JsonArray();

                                String sDesc = sObj.has("desc") ? sObj.get("desc").getAsString() : desc;

                                String chair = extractChair(sDesc);
                                String trilha = extractTrilha(name, trackNames);
                                String codigo = extractCodigo(name);
                                String titulo = extractTitulo(name);

                                JsonObject sessionJson = new JsonObject();
                                sessionJson.addProperty("data", "02/10/2026");
                                sessionJson.addProperty("trilha", trilha);
                                sessionJson.addProperty("codigo", codigo);
                                sessionJson.addProperty("titulo", titulo);
                                sessionJson.addProperty("horarioInicio", startTime);
                                sessionJson.addProperty("horarioFim", endTime);
                                sessionJson.addProperty("sala", place);
                                sessionJson.addProperty("evento", "SBGAMES");
                                sessionJson.addProperty("chair", chair);
                                sessionJson.addProperty("quantidadePapers", programs.size());
                                sessionJson.addProperty("quantidadeSubsessions", programs.size());

                                JsonArray papersArray = new JsonArray();
                                for (JsonElement pElem : programs) {
                                    JsonObject pObj = pElem.getAsJsonObject();
                                    String paperTitle = pObj.has("name") ? pObj.get("name").getAsString() : "";
                                    String pStart = pObj.has("start_time") ? extractTimeOnly(pObj.get("start_time").getAsString()) : startTime;
                                    String pEnd = pObj.has("end_time") ? extractTimeOnly(pObj.get("end_time").getAsString()) : endTime;
                                    String pDesc = pObj.has("desc") ? pObj.get("desc").getAsString() : "";

                                    String paperLink = extractLink(pDesc);
                                    String autoresText = "";

                                    if (paperLink != null && !paperLink.isEmpty()) {
                                        autoresText = extractAuthorsFromArticleUrl(paperLink);
                                    }
                                    if (autoresText.isEmpty()) {
                                        autoresText = extractAuthorsFromDesc(pDesc);
                                    }

                                    JsonObject paperJson = new JsonObject();
                                    paperJson.addProperty("titulo", paperTitle);
                                    paperJson.addProperty("link", paperLink != null ? paperLink : "");
                                    paperJson.addProperty("horarioInicio", pStart);
                                    paperJson.addProperty("horarioFim", pEnd);
                                    paperJson.addProperty("autores", autoresText);

                                    papersArray.add(paperJson);
                                }

                                sessionJson.add("papers", papersArray);
                                return sessionJson;

                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        }, EXECUTOR));
                    }
                }
            }
        }

        for (CompletableFuture<JsonObject> sf : sessionFutures) {
            JsonObject sJson = sf.get();
            if (sJson != null) {
                outputArray.add(sJson);
            }
        }

        return outputArray;
    }

    private static String fetchHttpResponse(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        conn.setRequestProperty("Accept", "application/json, text/plain, */*");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static String extractAuthorsFromArticleUrl(String url) {
        try {
            Document doc = fetchDocumentWithSslBypass(url);
            Elements authorLis = doc.select("ul.authors li, ul.item.authors li");
            if (!authorLis.isEmpty()) {
                List<String> authorList = new ArrayList<>();
                for (Element li : authorLis) {
                    Element nameElem = li.selectFirst("span.name");
                    Element affElem = li.selectFirst("span.affiliation");

                    String name = nameElem != null ? nameElem.text().trim() : "";
                    String aff = affElem != null ? affElem.text().trim() : "";

                    if (name.isEmpty()) {
                        continue;
                    }

                    if (!aff.isEmpty()) {
                        authorList.add(name + " (" + aff + ")");
                    } else {
                        authorList.add(name);
                    }
                }
                if (!authorList.isEmpty()) {
                    return String.join("; ", authorList);
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static Document fetchDocumentWithSslBypass(String url) throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        };
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());

        return Jsoup.connect(url)
                .sslSocketFactory(sc.getSocketFactory())
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(15000)
                .get();
    }

    private static String extractChair(String descHtml) {
        if (descHtml == null || descHtml.isEmpty()) return "";
        Document doc = Jsoup.parse(descHtml);
        Elements paragraphs = doc.select("p");
        for (Element p : paragraphs) {
            String text = p.text();
            if (text.toLowerCase().startsWith("chair:")) {
                return text.replaceFirst("(?i)chair:\\s*", "").trim();
            }
        }
        String text = doc.text();
        Pattern pattern = Pattern.compile("Chair:\\s*([^\\n<]+?)(?=\\s*\\d+\\s*papers|$)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private static String extractTrilha(String fullName, List<String> tracks) {
        Pattern pattern = Pattern.compile("\\[Trilha\\s+([^\\]]+)\\]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(fullName);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        if (!tracks.isEmpty()) {
            return tracks.get(0);
        }
        return "";
    }

    private static String extractCodigo(String fullName) {
        Pattern pattern = Pattern.compile("(ST\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(fullName);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private static String extractTitulo(String fullName) {
        String title = fullName.replaceAll("(?i)\\[Trilha[^\\]]+\\]", "").trim();
        int colonIdx = title.indexOf(":");
        if (colonIdx != -1) {
            return title.substring(colonIdx + 1).trim();
        }
        return title;
    }

    private static String extractTimeOnly(String fullTime) {
        if (fullTime == null) return "";
        if (fullTime.contains(" ")) {
            String[] parts = fullTime.split(" ");
            return parts[parts.length - 1];
        }
        return fullTime;
    }

    private static String extractLink(String descHtml) {
        if (descHtml == null || descHtml.isEmpty()) return null;
        Document doc = Jsoup.parse(descHtml);
        Element a = doc.selectFirst("a[href]");
        if (a != null) {
            return a.attr("href");
        }
        return null;
    }

    private static String extractAuthorsFromDesc(String descHtml) {
        if (descHtml == null || descHtml.isEmpty()) return "";
        Document doc = Jsoup.parse(descHtml);
        String text = doc.text();
        Pattern pattern = Pattern.compile("Autores:\\s*(.*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
}