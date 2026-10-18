#!/bin/sh
# คอมไพล์โปรเจกต์ (ต้องมี JDK 17 ขึ้นไป)
set -e
rm -rf out && mkdir out
javac -encoding UTF-8 -cp "lib/*" -sourcepath src -d out src/library/Main.java test/library/TestRunner.java
echo "Build OK"
