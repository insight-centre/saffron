#!/bin/bash

mkdir -p models
curl http://opennlp.sourceforge.net/models-1.5/en-token.bin -o models/en-token.bin
curl http://opennlp.sourceforge.net/models-1.5/en-chunker.bin -o models/en-chunker.bin
curl http://opennlp.sourceforge.net/models-1.5/en-pos-maxent.bin -o models/en-pos-maxent.bin

