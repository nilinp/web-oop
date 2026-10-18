#!/bin/sh
# รันเว็บ: ./run.sh [port]   (ค่าเริ่มต้น 8080)
java -cp "out:lib/*" library.Main "$@"
