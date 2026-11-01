"""Copies the Ponder scene title and texts (in order) into en_us.json as Ponder's lang keys.
Ponder reads <modid>.ponder.<scene>.header / .text_N at runtime. Run: python -I tools/sync_ponder_lang.py"""
import json
import re

SCENE = 'src/main/java/io/github/rick007110/displaylinkplus/client/ponder/MultiLinkScenes.java'
LANG = 'src/main/resources/assets/create_displaylinkplus/lang/en_us.json'
PREFIX = 'create_displaylinkplus.ponder.multi_link.'

source = open(SCENE, encoding='utf-8').read()
title = re.search(r'scene\.title\("multi_link", "(.*?)"\)', source).group(1)
texts = [t.replace('\\"', '"') for t in re.findall(r'\.text\("((?:[^"\\]|\\.)*)"\)', source)]

lang = json.load(open(LANG, encoding='utf-8'))
for key in [k for k in lang if k.startswith(PREFIX)]:
    del lang[key]
lang[PREFIX + 'header'] = title
for i, text in enumerate(texts, 1):
    lang[PREFIX + f'text_{i}'] = text
with open(LANG, 'w', encoding='utf-8', newline='\n') as f:
    f.write(json.dumps(lang, indent=2, ensure_ascii=False) + '\n')
print(len(texts), 'texts')
