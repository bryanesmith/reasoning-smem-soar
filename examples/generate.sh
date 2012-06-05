#!/bin/bash
# --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- 
#
# Generates declarative add files (KB) and initialization (agent) for 
# reasoning agent testing.
#
# Place the following in this directory:
#
#   * Onto2SMem: http://bryanesmith.com/soar/inference/downloads/onto2smem-bin.zip (Onto2SMem.jar and lib/)
#
#   * OWLFamiliesGenerator: http://bryanesmith.com/soar/inference/downloads/owl-families-generator.zip (owl-generator.jar)
#
#   * SoarQueryFamiliesGenerator: http://bryanesmith.com/soar/inference/downloads/soar-query-families-generator.zip (query-generator.jar)
#
# Change the variables below to generate test agents.
#
# Bryan Smith - Sat May 16 2010
# --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- 

# Set absolute path for current dirctory (PWD) and directory for output files (OUT_DIR)
PWD="/home/bryan/Temp/inference-query-count"
OUT_DIR="/data/Sources/Soar/family-reasoner/trial-files"

# Add each family count want to test
declare -a Families
Families[0]=1
Families[1]=5
Families[2]=10
Families[3]=15
Families[4]=20
Families[5]=25
Families[6]=50
Families[7]=100

# Add each total number of queries want to test
declare -a Queries
Queries[0]=1
Queries[1]=5
Queries[2]=10
Queries[3]=15
Queries[4]=20
Queries[5]=25
Queries[6]=50
Queries[7]=100

# What is the total number of trials per condition
TRIALS=3

rm -rf $OUT_DIR
mkdir $OUT_DIR

for Family in ${Families[@]}; do

   echo "--- --- --- --- --- --- --- --- --- --- --- ---"
   echo " Family count: $Family"
   echo "--- --- --- --- --- --- --- --- --- --- --- ---"

   for Query in ${Queries[@]}; do

      echo "--> Query count: $Query"

      Trial=0

      while [ $Trial -lt $TRIALS ]; do

         Trial=`expr $Trial + 1`

         echo "      - Trial: $Trial"

         SUFFIX="$Family-families-$Query-queries-$Trial-trial"
         OWL="$OUT_DIR/family.owl.$SUFFIX"
         FIRSTLOAD="$OUT_DIR/_firstload.$SUFFIX"
         
         # Generate the owl file
         java -jar owl-generator.jar -f $Family > $OWL

         # Generate the declarative add file
         echo "smem --set learning on" > $FIRSTLOAD
         echo "" >> $FIRSTLOAD
         java -jar Onto2SMem.jar "file://$OWL" >> $FIRSTLOAD

         # Generate the initialization file
         java -jar query-generator.jar -f $Family -q $Query > $OUT_DIR/initialize-family-reasoner.$SUFFIX

      done

   done
   
done

