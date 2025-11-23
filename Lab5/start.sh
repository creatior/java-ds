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

iters=(1 2 3 4 5 6 7 8 9 10)
procs=(1 2 3 4 5 6 7 8 9 10 11 12)

echo "=== Running tests ==="
for p in "${procs[@]}"; do
    for n in "${iters[@]}"; do
        echo "Processes: $p"
        $MPJ_HOME/bin/mpjrun.sh -np $p -Xmx2G -XX:MaxDirectMemorySize=4G $CLASS_NAME
        if [ $? -ne 0 ]; then
            echo "Run error: procs=$p, N=$n"
        fi
    done
done

echo "Results saved to: $OUTPUT_FILE"
