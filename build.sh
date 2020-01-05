#!/bin/bash
mkdir temp5678
javac -encoding utf8 -cp ./src -d ./temp5678 ./src/com/lx/Main.java
jar cfm c0Compiler.jar ./src/META-INF/MANIFEST.MF -C ./temp5678 .
chmod -x c0Compiler.jar
rm -rf ./temp5678
