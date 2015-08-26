scraper
========

scraper.jar - утилита для примитивного web-scraping'а по ключевым словам,
список ключевых слов передается в командной строке, разделитель - запятая.

Интерфейс командной строки поддерживает следующий набор опций:

# java -jar scraper.jar -help

	Usage: Scraper [URL | PATH] [WORD]... [-v verbose] [-w words count] [-c chars count] [-e matched sentences]

    [URL | PATH] - URL или/и путь к файлу со списком URL;
    [WORD]...    - список ключевых слов по формату: word1,.. wordN;
    -v           - поддержка debug-вывода в консоль (stderr, не меняла
                   это значение по умолчанию в стандартной java.util.logging lib);
    -w           - вывод на stdout количества слов для каждого обработанного web-документа;
    -c           - вывод на stdout количества символов для каждого обработанного web-документа;
    -e           - вывод на stdout предложений, содержащих ключи.

детали реализации
==================

1. Поиск и подсчет ключевых слов ведется только для блоков-контейнеров
   текста. Блоки, содержащие теги <script>, <object>, <video> и т.п.
   удаляются при парсинге.

2. Т.к. не используются сторонние библиотеки, парсер аргументов командной
   строки очень примитивный.

   В данный момент он не поддерживает возможность передачи списка URL через
   командную строку, т.е. можно передавать только один URL и/или путь к файлу
   со списком URL.

   Варианты последовательностей аргументов, на которых тестировался парсер
   командной строки:

        java -jar scraper.jar -vcwe https://docs.oracle.com/javase/8/ java,download

        java -jar scraper.jar -v -c -w -e https://docs.oracle.com/javase/8/ /tmp/links.txt java,download

        java -jar scraper.jar /home/calypso/java/links.txt java,download -vc -we

3. При парсинге текстовых блоков по предложениям в качестве значения локали
   используется текущая системная локаль (определяется стандартным вызовом
   Locale.getDefault()), т.е. значение кодировки веб-страницы не учитывается.

   Web-scraper тестировался при условиях, когда :

   # locale | grep LANG
   LANG=en_US.UTF-8
   LANGUAGE=en_US:en

   и значения атрибута charset HTML-страницы - "UTF-8";
   Поиск на страницы в кодировках, отличных от UTF-8 не поддерживается:

   # locale | grep LANG
   LANG=ru_RU.utf8
   LANGUAGE=ru_RU.utf8

   # java -jar scraper.jar http://www.gazeta.ru газ -cwe
   URL >> http://www.gazeta.ru

   	count of chars: 26365;
   	count of words: 2049;


   TOTAL >> count of chars: 26365;
   TOTAL >> count of words: 2049;

   KEYWORD >> 'газ' appears 0 times on all pages.

примеры запуска и результаты работы
====================================

1. # java -jar scraper.jar https://docs.oracle.com/javase/8/ java,download -c -we
   URL >> https://docs.oracle.com/javase/8/

   	count of chars: 1770;
   	count of words: 221;


   TOTAL >> count of chars: 1770;
   TOTAL >> count of words: 221;

   KEYWORD >> 'download' appears 2 times on all pages.

   	It was found in following sentences:

   	>> 'About Java SE 8         What&#39;s New (Features and Enhancements)      Commercial Features       Compatibility Guide      Known Issues         Download and Install         Certified System Configurations      Download and Installation Instructions         Write Your First Application         Get Started with Java      Get Started with JavaFX'

   KEYWORD >> 'java' appears 14 times on all pages.

   	It was found in following sentences:

   	>> 'Java EE DocumentationJava ME DocumentationJava DB DocumentationJava Components DocumentationJava Card Documentation'
   	>> 'Reference         Java SE API Documentation      JavaFX API Documentation      Developer Guides      Java Language and Virtual Machine Specifications      Java SE Tools Reference for UNIX      Java SE Tools Reference for Windows         Release Notes         Java SE Release Notes'
   	>> 'Java TrainingJava ForumsJava Source blogJava Tutorials blog'
   	>> 'Oracle Technology NetworkJava SE on OTNJava SE DownloadsJava SE Advanced and Java SE Suite'
   	>> 'About Java SE 8         What&#39;s New (Features and Enhancements)      Commercial Features       Compatibility Guide      Known Issues         Download and Install         Certified System Configurations      Download and Installation Instructions         Write Your First Application         Get Started with Java      Get Started with JavaFX'
   	>> 'Search Java SE Documentation'
   	>> 'Learn the Language         Java Tutorials Learning Paths         Monitor and Troubleshoot         Java Mission Control       Java Flight Recorder       Troubleshooting Guide      HotSpot Virtual Machine Garbage Collection Tuning Guide         Deploy         Deployment Guide'

2. # java -jar scraper.jar -c -w -e http://www.linux-magazine.com/Issues/2014/163/Java-8/\(language\)/eng-US /tmp/links.txt java,download

URL >> http://www.linux-magazine.com/Issues/2014/163/Java-8/(language)/eng-US

	count of chars: 12022;
	count of words: 1540;

URL >> http://google.com
Aug 23, 2015 2:07:41 PM Scraper main
WARNING: Error while process page: 'http://google.com':
java.lang.Exception: Will skip processing: 'http://google.com', exit code 302, while opening.
	at URLReader.getURLContent(Scraper.java:54)
	at Scraper.main(Scraper.java:341)

TOTAL >> count of chars: 12022;
TOTAL >> count of words: 1540;

KEYWORD >> 'download' appears 1 times on all pages.

	It was found in following sentences:

	>> 'Direct Download	    	    		    Read full article as PDF:'

KEYWORD >> 'java' appears 25 times on all pages.

	It was found in following sentences:

	>> 'For example, Java 8 uses it in its well-known  interface (Listing 1).'
	>> 'In Java 8, these ideas are realized directly in the host language Java. '
	>> 'Not only does it contain minor enhancements to the runtime library, it sees functional elements enter the Java universe in the form of lambdas.PostponedDevelopment of the new version was fraught with the same kind of issues as were experienced in the previous release: In August 2010, Reinhold pulled the ripcord and reduced the planned Java 7 feature scope so it could be completed in July 2011. '
	>> 'super U&gt; keyComparator)  {14         [...]15     }16      // Java 8: Default implementation in the interface17     default Comparator&lt;T&gt; reversed() {18         return Collections.reverseOrder(this);19     }20 }'
	>> 'In addition to small tweaks, the long-awaited release extends the core language, adding elements of functional programming – the first significant development since Java 5.	            		    	        '
	>> 'Lambdas in Java 8    01 List&lt;Rectangle&gt; foursides = [...]0203  // Java 1-7: Anonymous, inner class04 Comparator&lt;Rectangle&gt; compr7 = new Comparator&lt;Rectangle&gt;() {05     @Override06     public int compare(Rectangle o1, Rectangle o2) {07         return o1.width - o2.width;08     }09 };10 foursides.sort (compr7);1112 // Java 8: Short definition as a Lambda13 foursides.sort( (Rectangle r1, Rectangle r2) -&gt; r1.width - r2.width);14 foursides.sort( (r1, r2) -&gt; r1.width - r2.width);1516 // Java 8: Use of static methods and references17 foursides.sort(Comparator.comparingDouble(Rectangle::getWidth));18 // Java 8: Use of default implementation from interface19 foursides.sort(Comparator.comparingDouble(Rectangle::getWidth).reversed().20         thenComparing(Comparator.comparingDouble(Rectangle::getHeight)));'
	>> 'Home        	            	    		&nbsp;&#187;&nbsp;	    			    	    	    	    	    	    	    	        	        Issues	    		            	    		&nbsp;&#187;&nbsp;	    			    	    	    	    	    	    	    	        	        2014	    		            	    		&nbsp;&#187;&nbsp;	    			    	    	    	    	    	    	    	        	        163	    		            	    		&nbsp;&#187;&nbsp;	    			    	    	    	    	    	    	    	        Java 8'
	>> 'In addition to small tweaks, the long-awaited release extends the core language, adding elements of functional programming – the first significant development since Java 5.'
	>> 'The language and Java Runtime Environment (JRE) were actually planned from the beginning to prevent malicious code from breaking out of the designated sandbox, but the implementation of the concepts had significant weaknesses, so many Windows users made the unwelcome acquaintance of the BKA ransomware installed by a Java applet [2].In particular, the planned modularization of JRE fell victim to the work of plugging security gaps (Project Jigsaw) [3]. '
	>> 'After two and a half years of work, long-serving Java development chief Mark Reinhold released version 8 of Java in March. '
	>> 'In addition to the Lambdas, the syntax for Java 8 has been extended by two additional features: static methods defined in the interface and in the default implementation.An example of the static method definition is shown in line 17 of Listing 2 The Lambda definition of the comparator from line 14 is nice and short, but you already know how to compare two numbers, so you don't need to reinvent the wheel to do that.A ready-made implementation for this kind of trivial task can be defined as a static method in the interface in Java 8. '
	>> 'Java gets going with version 8'
	>> 'Java Comparator with FI    01 package java.util;0203 // Java 8: Definition as a FunctionalInterface04 @FunctionalInterface05 public interface Comparator&lt;T&gt; {0607     // This is THE method in the FunctionalInterface08     int compare(T o1, T o2);0910     //Java 8: Static methods with implementation in the interface11      public static &lt;T, U&gt; Comparator&lt;T&gt; comparing(12             Function&lt;? '
	>> 'The postponed features were due in version 8 at the end of 2012, which eventually became the beginning of 2014 – again with a reduced feature set.The reason for the delay was mainly the much needed improvement of the security deficiencies in applets and Java Web Start [1]. '
	>> 'Thanks to the ARM port, Java now runs smoothly on the Raspberry Pi, but you can hardly imagine an application on this little computer needing the entire Java standard library, including the Corba stack and LDAP support. '


TODO
=====

 -- переписать код классов Parser, Scraper с поддержкой итераторов и ф-ций высшего порядка;
 -- добавить в интерфейс командной строки поддержку списка URL;
 -- добавить поддержку поиска ключевых слов на локализованных веб-страницах.