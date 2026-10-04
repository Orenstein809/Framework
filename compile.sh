#!/bin/bash

echo "========================================="
echo "Compilation du Framework Java 21 (JAR)"
echo "========================================="

# Nettoyage et packaging via Maven
mvn clean package

if [ $? -ne 0 ]; then
    echo ""
    echo "[ERREUR] La compilation a échoué. Veuillez vérifier les erreurs ci-dessus."
    read -p "Appuyez sur Entrée pour continuer..."
    exit 1
fi

echo ""
echo "[SUCCÈS] Le framework a été compilé avec succès !"
echo "Le fichier JAR se trouve dans le dossier 'target/'."
read -p "Appuyez sur Entrée pour continuer..."
