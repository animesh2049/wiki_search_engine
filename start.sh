javac -d out/production/wiki_search_engine src/Wiki/*.java
cd out/production/wiki_search_engine
java -Xmx512m Wiki.Main $1 
