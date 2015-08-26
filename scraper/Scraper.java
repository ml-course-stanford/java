/*
* main class Scraper --> implements simple webscraper function
* to crunch keywords from URLs defined by command line interface;
*
* class URLReader --> implements webpages Reader;
*
* class Parser --> implements web-pages parser for splitting
* text into blocks and obtaining list of sentences;
*
* */

import java.io.*;

import java.lang.Boolean;
import java.lang.String;

import java.nio.file.*;
import java.net.*;

import java.text.BreakIterator;

import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.logging.SimpleFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


class URLReader{
    public static String getURLContent(String url) throws Exception {

        URL website = new URL(url);

        HttpURLConnection connection = (HttpURLConnection)website.openConnection();
        connection.setRequestMethod("GET");

        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36";
        connection.setRequestProperty("User-Agent", userAgent);

        connection.connect();
        int code = connection.getResponseCode();

        if (code!=200){
            throw new Exception ("Will skip processing: '"+url+"', exit code "+code+", while opening.");
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        connection.getInputStream()));

        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null)
            response.append(inputLine);

        in.close();

        return response.toString();
    }
}


class Parser{

    private static ArrayList<String> getFilter (ArrayList<String> rawLines, ArrayList<String> regexpLines, Boolean replaceFlag){

        ArrayList<String> filteredLines = new ArrayList<String>();

        for (String line : rawLines) {
            for (String r : regexpLines) {

                if (replaceFlag){
                    line = line.replaceAll(r,"");
                }else {

                    Pattern p = Pattern.compile(r);
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        break;
                    }
                }
            }
            filteredLines.add(line);
        }
        return filteredLines;
    }

    public static List<String> getTextBlocks(String contentString) throws Exception {

        // 1. get content of webpage body as one line
        String line = contentString.replaceAll("(?is)</?html\\s?.*?>", "");

        String[] tags = {"head", "nav", "time"};
        for (String t : tags) {
            line = line.replaceAll("(?is)<" + t + "\\b.*?>.*?</" + t + ">", "");
        }

        //Pattern charsetRegexp = Pattern.compile("(?is)(?<=charset\=).*?(\s|/\>)");
        //String charset = "utf-8";
        //Matcher charsetMatcher = charsetRegexp.matcher(headLine);
        //if (charsetMatcher.find()){
        //    charset = m.group().replaceAll("(/s|\"|'", "");
        //}
        //LOGGER.fine("charset : " + charset );

        // 2. split content of body-line to text blocks, according to HTML-markup
        String[] divs = line.split("(?is)<div.*?>");

        ArrayList<String> divBlocks = new ArrayList<String>();
        for (String div : divs) {
            div = div.replaceAll("(\r?\n|</div>)", "");
            div = div.trim();
            if (div.length() == 0) {
                continue;
            }
            divBlocks.add(div);
        }

        // 3. remove blocks with media-content and script's code
        ArrayList<String> regexpList = new ArrayList<String>();

        String[] tagsToRemove = {"object", "embed", "svg", "video", "applet", "audio"};
        for (String tag : tagsToRemove) {
            String r = "(?i)<" + tag;
            regexpList.add(r);
        }

        ArrayList<String> blocks = getFilter(divBlocks, regexpList, Boolean.FALSE);

        // 4. replace non-text tags with empty line
        regexpList.clear();
        String[] tagsToErase = {"base", "form", "basefont", "canvas", "col", "colgroup", "(i|no)?frame(set|s)?", "ins",
                "map", "area", "kbd", "noscript", "progress", "samp", "code", "script", "select"};

        for (String tag : tagsToErase) {
            String r = "(?i)<" + tag + "\\b?.*?>.*?</" + tag + ">";
            regexpList.add(r);
        }

        String[] removeKeys = {"menu", "copyright", "button", "calendar"};
        for (String kwd : removeKeys) {
            String r = "(?i)(class|id)=(['\"])?.*?" + kwd + ".*?(['\"])?";
            regexpList.add(r);
        }

        ArrayList<String> textBlocks = getFilter(blocks, regexpList, Boolean.TRUE);

        regexpList.clear();
        regexpList.add("</?.*?>");
        ArrayList<String> blocksToTrim = getFilter(textBlocks, regexpList, Boolean.TRUE);

        ArrayList<String> parsedBlocks = new ArrayList<String>();
        for (String blk : blocksToTrim) {
            blk = blk.trim();
            if (blk.length() != 0) {
                parsedBlocks.add(blk);
            }
        }

        //Scraper.LOGGER.fine(" textBlocks : " + parsedBlocks);

        if (parsedBlocks.isEmpty()) {
            throw new Exception("Can't parse current page !");
        }
        return parsedBlocks;
    }

    public static List<String> getSentences(String paragraph) throws Exception {

        // could be changed according to opened webpage charset,

        Locale locale = Locale.getDefault();
        BreakIterator iterator = BreakIterator.getSentenceInstance(locale);

        iterator.setText(paragraph);

        List<String> sentences = new ArrayList<String>();
        int lastIndex = iterator.first();
        while (lastIndex != BreakIterator.DONE) {
            int firstIndex = lastIndex;
            lastIndex = iterator.next();

            if (lastIndex != BreakIterator.DONE) {
                String sentence = paragraph.substring(firstIndex, lastIndex);
                Scraper.LOGGER.fine("\tsentence >> " + sentence);
                sentences.add(sentence);
            }
        }
        if (sentences.size() == 0){
            throw new Exception("Can't parse text block into sentences !");
        }
        return sentences;

    }
}


class Scraper {

    public static final Logger LOGGER = Logger.getLogger(Scraper.class.getName());

    public static void main(String[] args) throws Exception {

        LOGGER.setUseParentHandlers(false);

        Handler consoleHandler = new ConsoleHandler();
        LOGGER.addHandler(consoleHandler);
        consoleHandler.setLevel(Level.ALL);

        int i = 0, j;
        char flag;
        boolean vflag = false, cflag = false, wflag = false, eflag = false;

        String arg, url = null, inputFile = null;
        HashSet<String> keys = new HashSet<String>();
        String usage = "\n\tUsage: Scraper [URL | PATH] [WORD]... [-v verbose] [-w words count] [-c chars count] [-e matched sentences]\n";

        // 1. parse command line
        while (i < args.length) {
            arg = args[i];

            // accepts only one URL from command line,
            // but parse file with URL's list from disk,
            // if the path to this file was defined
            if (arg.startsWith("http")) {
                url = arg;
            } else if (arg.startsWith("-")) {
                for (j = 1; j < arg.length(); j++) {
                    flag = arg.charAt(j);

                    switch (flag) {
                        case 'v':
                            LOGGER.setLevel(Level.ALL);
                            break;
                        case 'w':
                            wflag = true;
                            break;
                        case 'c':
                            cflag = true;
                            break;
                        case 'e':
                            eflag = true;
                            break;
                        case 'h':
                            System.out.println(usage);
                            System.exit(0);
                        default:
                            System.err.println("illegal option: " + flag);
                            System.err.println(usage);
                            System.exit(1);
                    }
                }
            } else {
                Path p = Paths.get(arg);
                if (Files.exists(p) && Files.isReadable(p)) {
                    inputFile = p.toString();
                } else {
                    keys.addAll(Arrays.asList(arg.split(",")));
                }
            }
            i++;
        }

        // verify arguments
        if (i == args.length) {

            if ((url == null) && (inputFile == null)) {
                System.err.println(usage);
                System.exit(1);
            } else if (keys.isEmpty()) {
                System.err.println("Please, provide some keywords to search.");
                System.err.println(usage);
                System.exit(1);
            }

            // 2. collect urls to open
            List<String> urls = new ArrayList<String>();

            if (url != null) {
                urls.add(url);
            }

            if (inputFile != null) {

                try {
                    FileInputStream fStream = new FileInputStream(inputFile);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fStream));

                    String line = null;
                    while ((line = br.readLine()) != null) {
                        urls.add(line.trim());
                    }
                    br.close();

                } catch (IOException e) {
                    LOGGER.warning("Error while reading '" + inputFile + "':");
                    e.printStackTrace();

                    if (urls.isEmpty()) {
                        LOGGER.warning("Don't have any URLs to proceed, quitting.");
                        System.exit(1);
                    } else {
                        LOGGER.warning("Will use only URL provided in command line.");
                    }
                }
            }

            // 3. process each page one by one
            Set<String> urlsSet = new HashSet<String>(urls);

            int totalCharsNum = 0;
            int totalWordsNum = 0;
            HashMap<String, Integer> keysCounts = new HashMap<String, Integer>();
            HashMap<String, HashSet<String>> keysSentences = new HashMap<String, HashSet<String>>();

            HashSet<String> tmpList = new HashSet<String>();
            for (String key : keys) {
                keysCounts.put(key, 0);
                keysSentences.put(key, tmpList);
            }

            for (String u : urlsSet) {
                if (u.length() == 0)
                    continue;

                System.out.println("URL >> " + u);

                List<String> sentences = new ArrayList<String>();
                int pageCharsNum = 0;
                int pageWordsNum = 0;

                try {
                    String content = URLReader.getURLContent(u);
                    // will obtain list of text blocks,
                    // don't parse JS- and others scripts code
                    List<String> blocks = Parser.getTextBlocks(content);

                    for (String blk : blocks) {
                        // will extract sentences from each text block
                        sentences.addAll(Parser.getSentences(blk));
                    }
                } catch (Exception e) {
                    LOGGER.warning("Error while process page: '" + u + "':");
                    e.printStackTrace();
                    continue;
                }

                // 4. search keywords and update counters for each sentence
                for (String s : sentences) {
                    LOGGER.fine("Sentence >> " + s);
                    pageCharsNum += s.length();

                    List<String> words = Arrays.asList(s.trim().split("\\s+"));
                    pageWordsNum += words.size();

                    for (String key : keys) {
                        for (String w : words) {
                            if (w.equalsIgnoreCase(key)) {
                                keysCounts.put(key, (keysCounts.get(key) + 1));
                                if (eflag) {
                                    HashSet<String> tmp = new HashSet<String>(keysSentences.get(key));
                                    tmp.add(s);
                                    keysSentences.put(key, tmp);

                                }
                            }
                        }
                    }
                }

                if (cflag) {
                    System.out.println("\n\tcount of chars: " + pageCharsNum + ";");
                }
                if (wflag) {
                    System.out.println("\tcount of words: " + pageWordsNum + ";\n");
                }
                // 5. update TOTAL counters
                totalCharsNum += pageCharsNum;
                totalWordsNum += pageWordsNum;

            }

            System.out.println("\nTOTAL >> count of chars: " + totalCharsNum + ";");
            System.out.println("TOTAL >> count of words: " + totalWordsNum + ";\n");

            for (String k : keys) {
                int count = keysCounts.get(k);
                System.out.println("KEYWORD >> '" + k + "' appears " + keysCounts.get(k) + " times on all pages.\n");
                if (count > 0 && eflag) {
                    System.out.println("\tIt was found in following sentences: \n");
                    for (String sent : keysSentences.get(k)) {
                        System.out.println("\t>> '" + sent + "'");
                    }
                    System.out.println();
                }
            }
        }
    }
}