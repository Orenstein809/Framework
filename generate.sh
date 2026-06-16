#!/bin/bash

# ======================
# Configuration
# ======================

PROJECT_NAME="Framework"
SRC_DIR="."
BUILD_DIR="build"
LIB_DIR="lib"

# ======================
# Nettoyage
# ======================

rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR/classes

# ======================
# Construction du classpath
# ======================

CLASSPATH=""

for jar in $LIB_DIR/*.jar
do
    CLASSPATH="$CLASSPATH:$jar"
done

# ======================
# Compilation
# ======================

find $SRC_DIR -name "*.java" > sources.txt

javac \
-cp "$CLASSPATH" \
-d $BUILD_DIR/classes \
@sources.txt

if [ $? -ne 0 ]; then
    echo "Erreur de compilation"
    exit 1
fi

# ======================
# Génération du JAR
# ======================

cd $BUILD_DIR/classes || exit

jar cvf ../../$PROJECT_NAME.jar .

cd ../..

rm sources.txt

echo ""
echo "JAR généré : $PROJECT_NAME.jar"
echo ""