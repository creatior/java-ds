#!/bin/bash
MPJ_HOME="/mnt/c/mpj"
export MPJ_HOME
CLASS_NAME="Main"
OUTPUT_FILE="results.csv"

MPJ_JAR="$MPJ_HOME/lib/mpj.jar"

javac -cp ".:$MPJ_JAR" -d . src/$CLASS_NAME.java
if [ $? -ne 0 ]; then
    echo "Compilation error."
    exit 1
fi

sizes=(10000 100000 1000000 10000000)
procs=(2 4 6 8 10)

echo "=== Running tests ==="
for p in "${procs[@]}"; do
    for n in "${sizes[@]}"; do
        echo "Processes: $p | N: $n"
        $MPJ_HOME/bin/mpjrun.sh -np $p -Xmx2G -XX:MaxDirectMemorySize=4G $CLASS_NAME $n
        if [ $? -ne 0 ]; then
            echo "Run error: procs=$p, N=$n"
        fi
    done
done

echo "Results saved to: $OUTPUT_FILE"
