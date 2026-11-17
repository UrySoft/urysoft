#!/usr/bin/env python3
"""Check complete resource parity, formatting and the compiled language variants."""
from pathlib import Path
import argparse,re,subprocess,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];res=root/'app/src/main/res';checks=0
def expect(ok,label):
 global checks
 checks+=1
 if not ok:raise AssertionError(label)
def entries(folder):
 tree=ET.parse(res/folder/'strings.xml').getroot();result={}
 for e in tree:
  key=(e.tag,e.attrib['name']);expect(key not in result,'duplicate '+str(key))
  result[key]={x.attrib['quantity']:x.text or '' for x in e} if e.tag=='plurals' else e.text or ''
 return result
maps={lang:entries(folder) for lang,folder in [('en','values'),('ca','values-ca'),('es','values-es')]}
base=maps['en'];format_pattern=r'%(?:\d+\$)?[0-9.]*[sdX]'
for lang,strings in maps.items():
 expect(strings.keys()==base.keys(),'resource parity '+lang)
 for key,value in strings.items():
  source=base[key]
  if isinstance(value,dict):
   expect(value.keys()==source.keys(),'plural forms '+str(key));pairs=[(source[q],t) for q,t in value.items()]
  else:pairs=[(source,value)]
  for a,b in pairs:
   expect(bool(b.strip('" ')),'nonempty '+str(key))
   expect(re.findall(format_pattern,a)==re.findall(format_pattern,b),'format arguments '+str(key))
known={name for typ,name in base if typ=='string'}
for p in (root/'app/src/main/java').rglob('*.java'):
 for name in re.findall(r'(?<!android\.)R\.string\.(\w+)',p.read_text(encoding='utf-8')):expect(name in known,'resource reference '+name)
# All view labels must be resources rather than inline XML text.
for p in res.rglob('*.xml'):
 for e in ET.parse(p).iter():
  for name,value in e.attrib.items():
   if name.rsplit('}',1)[-1] in {'text','hint','contentDescription'}:expect(value.startswith('@'),'inline label '+str(p))
config=ET.parse(res/'xml/locales_config.xml').getroot()
expect({e.attrib['{http://schemas.android.com/apk/res/android}name'] for e in config}=={'en','ca','es'},'supported locales')
expect('My shortcuts' in base[('string','default_widget_name')],'English default resources')
expect('Els meus accessos' in maps['ca'][('string','default_widget_name')],'Catalan default name')
expect('Mis accesos' in maps['es'][('string','default_widget_name')],'Spanish default name')
parser=argparse.ArgumentParser();parser.add_argument('--aapt2');parser.add_argument('--apk');args=parser.parse_args()
if args.aapt2 and args.apk:
 dump=subprocess.check_output([args.aapt2,'dump','resources',args.apk],text=True)
 for name in known:
  block=re.search(r'\bstring/'+re.escape(name)+r'\n(.*?)(?=    resource |\Z)',dump,re.S)
  expect(block is not None,'packaged resource '+name)
  for locale in ['', 'ca','es']:expect('('+locale+') ' in block.group(1),'packaged translation '+name+' '+locale)
 expect('"Titles"' in dump and '"Títols"' in dump and '"Títulos"' in dump,'compiled localized text')
print(f'PASS: {checks} localization checks; {len(known)} strings and notification plurals in English, Catalan and Spanish.')
