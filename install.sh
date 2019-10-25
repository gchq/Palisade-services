#!/bin/sh
dt=$(date '+%d/%m/%Y %H:%M:%S');
echo "$dt" > $PWD/mvn_dependency_tree.txt
mvn dependency:tree -DoutputFile=$PWD/mvn_dependency_tree.txt -DappendOutput=true
mvn install
