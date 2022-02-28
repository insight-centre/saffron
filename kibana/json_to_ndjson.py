import json
import sys
import os

input_file = sys.argv[1]
index_title = sys.argv[2]
output_file = sys.argv[3]

# ofile_name = ifile_name.split(".")[0] + "_elk." + ifile_name.split(".")[1]
# ofile_path = DIR_PATH + "/" +  ofile_name

with open(input_file) as f:
    data = json.load(f)

with open(output_file, "w")as f:
    for i, item in enumerate(data):
        index_dict = {"index":{"_index":index_title,"_id":i}}
        json.dump(index_dict,f)
        f.write("\n")
        json.dump(item,f)
        f.write("\n")
