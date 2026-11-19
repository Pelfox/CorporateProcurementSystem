from pathlib import Path
import re
import json
from pypdf import PdfReader
import pdfplumber
from PIL import Image, ImageOps, ImageDraw, ImageFont
from reportlab.pdfbase.ttfonts import TTFont

ROOT=Path(__file__).resolve().parents[2]
TMP=ROOT/'tmp/pdfs'
SOURCE=TMP/'guide.md'
PDF=ROOT/'output/pdf/java-spring-defense-guide.pdf'
source=SOURCE.read_text()
questions=[int(x) for x in re.findall(r'^\*\*(\d+)\.',source,re.M)]
assert questions==list(range(1,201)), questions
assert len(re.findall(r'^# \d\d\.',source,re.M))==38
reader=PdfReader(PDF)
assert len(reader.outline)==38
all_text='\n'.join(p.extract_text() for p in reader.pages)
for term in ['Spring Data', 'N+1', 'JDBC', 'REQUIRES_NEW', '200.', '37. Практические']:
    assert term in all_text, term
assert '\ufffd' not in all_text
assert '\u25a0' not in all_text
links=sum(len(p.get('/Annots',[])) for p in reader.pages)
chapters={x.title:reader.get_destination_page_number(x)+1 for x in reader.outline}

font=TTFont('CheckArial','/System/Library/Fonts/Supplemental/Arial.ttf')
glyphs=set(font.face.charToGlyph)
missing=sorted({c for c in source if ord(c)>32 and ord(c) not in glyphs})
assert not missing, missing
overflow=[]; low_pages=[]
with pdfplumber.open(PDF) as doc:
    for n,p in enumerate(doc.pages,1):
        if n==1: continue
        chars=p.chars
        bad=[c for c in chars if c['x0']<40 or c['x1']>p.width-40 or c['top']<17 or c['bottom']>p.height-17]
        if bad: overflow.append((n, ''.join(c['text'] for c in bad)[:100]))
        body=[c for c in chars if 60<c['top']<p.height-60]
        if len(body)<150: low_pages.append((n,len(body)))
assert not overflow, overflow

files=sorted(TMP.glob('page-*.jpg'))
assert len(files)==len(reader.pages), (len(files), len(reader.pages))
label_font=ImageFont.truetype('/System/Library/Fonts/Supplemental/Arial.ttf',15)
for k in range(0,len(files),16):
    batch=files[k:k+16]
    sheet=Image.new('RGB',(4*340,4*480),(222,230,235))
    draw=ImageDraw.Draw(sheet)
    for j,path in enumerate(batch):
        im=Image.open(path).convert('RGB')
        im.thumbnail((322,450))
        x=(j%4)*340+(340-im.width)//2
        y=(j//4)*480+23
        sheet.paste(im,(x,y))
        draw.text(((j%4)*340+12,(j//4)*480+3),f'Страница {k+j+1}',font=label_font,fill=(18,43,67))
    sheet.save(TMP/f'contact-{k//16+1}.jpg',quality=90)

report={'pages':len(reader.pages),'questions':len(questions),'chapters':len(chapters),
        'word_count':len(source.split()),'links':links,'low_text_pages':low_pages,
        'chapters_at_pages':chapters,'size_bytes':PDF.stat().st_size}
(TMP/'qa-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
(TMP/'extracted.txt').write_text(all_text)
print(json.dumps(report,ensure_ascii=False,indent=2))
