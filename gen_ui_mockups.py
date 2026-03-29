#!/usr/bin/env python3
from PIL import Image, ImageDraw, ImageFont
import os, math

OUT_DIR = '/Users/zhaoyu/Desktop/UI_Mockups'
os.makedirs(OUT_DIR, exist_ok=True)

W, H = 1200, 800
BG = (248, 249, 250)
NAVY = (46, 64, 87)
TEAL = (4, 138, 129)
LIGHT_BLUE = (24, 144, 255)
ORANGE = (250, 140, 22)
GREEN = (82, 196, 26)
GRAY = (200, 200, 200)
DGRAY = (100, 100, 100)
WHITE = (255, 255, 255)
RED = (245, 34, 45)
PURPLE = (114, 46, 209)
LGRAY = (240, 240, 240)
BORDER = (220, 220, 220)

def new_img():
    img = Image.new('RGB', (W, H), WHITE)
    return img, ImageDraw.Draw(img)

def rect(d, xy, fill=None, outline=None, width=1):
    d.rectangle(xy, fill=fill, outline=outline, width=width)

def rrect(d, xy, r=8, fill=None, outline=None, w=1):
    x0,y0,x1,y1 = xy
    d.rectangle([x0+r,y0,x1-r,y1], fill=fill, outline=outline, width=w)
    d.rectangle([x0,y0+r,x1,y1-r], fill=fill, outline=outline, width=w)
    d.pieslice([x0,y0,x0+2*r,y0+2*r], 180,270, fill=fill, outline=outline)
    d.pieslice([x1-2*r,y0,x1,y0+2*r], 270,360, fill=fill, outline=outline)
    d.pieslice([x0,y1-2*r,x0+2*r,y1], 90,180, fill=fill, outline=outline)
    d.pieslice([x1-2*r,y1-2*r,x1,y1], 0,90, fill=fill, outline=outline)

def txt(d, pos, s, fill=DGRAY, sz=11):
    try: f = ImageFont.truetype('/System/Library/Fonts/Helvetica.ttc', sz)
    except: f = ImageFont.load_default()
    d.text(pos, s, fill=fill, font=f)

def txtc(d, cx, y, s, fill=DGRAY, sz=11):
    try: f = ImageFont.truetype('/System/Library/Fonts/Helvetica.ttc', sz)
    except: f = ImageFont.load_default()
    bb = d.textbbox((0,0),s,font=f); ww=bb[2]-bb[0]
    d.text((cx-ww//2,y), s, fill=fill, font=f)

def tag(d, x, y, s, fill=GREEN):
    try: f=ImageFont.truetype('/System/Library/Fonts/Helvetica.ttc',9)
    except: f=ImageFont.load_default()
    bb=d.textbbox((0,0),s,font=f); ww=bb[2]-bb[0]+12
    rect(d,[x,y,x+ww,y+20],fill=fill)
    d.text((x+6,y+3),s,fill=WHITE,font=f)

def badge(d, x, y, s, fill=RED):
    try: f=ImageFont.truetype('/System/Library/Fonts/Helvetica.ttc',9)
    except: f=ImageFont.load_default()
    bb=d.textbbox((0,0),s,font=f); ww=bb[2]-bb[0]+8
    rect(d,[x,y,x+ww,y+20],fill=fill)
    d.text((x+4,y+3),s,fill=WHITE,font=f)

def pbar(d, x, y, w, h, pct, fill=TEAL):
    rect(d,[x,y,x+w,y+h],fill=LGRAY,outline=BORDER,w=1)
    pw=int(w*pct/100)
    if pw>0: rect(d,[x,y,x+pw,y+h],fill=fill)

def layout_base(d, title='机构绩效管理系统', user='管理员：张三  |  退出', active=1):
    rect(d,[0,0,W,60],fill=NAVY)
    txt(d,(30,20),title,fill=WHITE,sz=16)
    badge(d,620,20,'5',RED)
    txt(d,(700,22),user,fill=(180,200,220),sz=10)
    rect(d,[0,60,200,H],fill=WHITE,outline=BORDER,w=1)
    menus=['首页仪表盘','考核体系管理','月度考核监测','绩效结果展示','通知中心']
    for i,m in enumerate(menus):
        y=80+i*50
        bg=TEAL if i==active else None
        fc=WHITE if i==active else DGRAY
        if bg: rect(d,[0,y,200,y+44],fill=bg)
        txt(d,(20,y+14),m,fill=fc,sz=11)
    return 220

def table_header(d, x, y, headers, cws):
    rect(d,[x,y,x+sum(cws),y+36],fill=NAVY)
    xx=x
    for h,cw in zip(headers,cws):
        txt(d,(xx+4,y+10),h,fill=WHITE,sz=9)
        xx+=cw
    return y+36

def table_row(d, y, vals, cws, bg=None, tags=None):
    if bg is None: bg=WHITE
    xx=x0 if 'x0' in dir() else 0
    x0_local=0
    for i,(v,cw) in enumerate(zip(vals,cws)):
        if i==0: x0_local=xx
        rect(d,[xx,y,xx+cw,y+44],fill=bg,outline=BORDER,w=1)
        if tags and i in tags:
            tag(d,xx+4,y+12,v,fill=tags[i])
        else:
            txt(d,(xx+4,y+14),str(v)[:int(cw/5)],fill=DGRAY,sz=8)
        xx+=cw
    return y+44

# ---- 登录页 ----
def draw_login():
    img,d=new_img()
    rect(d,[0,0,W,H],fill=BG)
    rect(d,[0,0,500,H],fill=NAVY)
    txtc(d,250,180,'机构绩效管理系统',fill=WHITE,sz=28)
    txtc(d,250,225,'Institution Performance',fill=(150,170,190),sz=16)
    txtc(d,250,255,'Management System',fill=(150,170,190),sz=16)
    txtc(d,250,340,'数字化赋能分行绩效考核',fill=(150,170,190),sz=13)
    d.ellipse([350,480,500,630],fill=(60,80,110))
    d.ellipse([30,560,200,730],fill=(60,80,110))
    cx,cy,cw,ch=750,140,400,520
    rrect(d,[cx,cy,cx+cw,cy+ch],r=12,fill=WHITE,outline=BORDER,w=1)
    txt(d,(cx+40,cy+40),'用户登录',fill=NAVY,sz=20)
    txt(d,(cx+40,cy+80),'请输入您的账号信息',fill=DGRAY,sz=11)
    txt(d,(cx+40,cy+120),'用户名（工号）',fill=DGRAY,sz=10)
    rect(d,[cx+40,cy+140,cx+320,cy+180],fill=LGRAY,outline=BORDER,w=1)
    txt(d,(cx+50,cy+155),'请输入工号',fill=(160,160,160),sz=11)
    txt(d,(cx+40,cy+200),'密码',fill=DGRAY,sz=10)
    rect(d,[cx+40,cy+220,cx+320,cy+260],fill=LGRAY,outline=BORDER,w=1)
    txt(d,(cx+50,cy+235),'••••••••',fill=(160,160,160),sz=11)
    rect(d,[cx+40,cy+290,cx+320,cy+340],fill=TEAL)
    txtc(d,cx+180,cy+307,'登 录',fill=WHITE,sz=14)
    txtc(d,cx+cw//2,cy+ch-30,'机构绩效管理系统 V1.0',fill=(180,180,180),sz=9)
    img.save(OUT_DIR+'/01_login.png')
    print('01_login.png done')

# ---- 首页仪表盘 ----
def draw_dashboard():
    img,d=new_img()
    rect(d,[0,0,W,H],fill=BG)
    cx=layout_base(d)
    txtc(d,cx+580,80,'首页仪表盘',fill=NAVY,sz=18)
    txt(d,(cx+20,110),'欢迎回来，张三',fill=DGRAY,sz=11)
    stats=[('进行中监测','3',TEAL),('待确认','1',ORANGE),('已发布','12',GREEN),('我的待办','5',PURPLE)]
    for i,(t,n,col) in enumerate(stats):
        x=cx+20+i*238; y=145
        rrect(d,[x,y,x+218,y+110],r=8,fill=WHITE,outline=BORDER,w=1)
        rect(d,[x,y,x+218,y+4],fill=col)
        txt(d,(x+15,y+20),t,fill=DGRAY,sz=11)
        txt(d,(x+15,y+50),n,fill=col,sz=30)
        txt(d,(x+85,y+60),'个',fill=DGRAY,sz=11)
    txt(d,(cx+20,280),'快捷入口',fill=NAVY,sz=14)
    btns=[('发起月度监测',TEAL),('填写绩效数据',LIGHT_BLUE),('查看绩效报表',PURPLE),('通知中心',ORANGE)]
    for i,(b,bc) in enumerate(btns):
        x=cx+20+i*200; y=310
        rrect(d,[x,y,x+180,y+60],r=6,fill=bc)
        txtc(d,x+90,y+22,b,fill=WHITE,sz=11)
    rrect(d,[cx+20,430,cx+20+1080,730],r=8,fill=WHITE,outline=BORDER,w=1)
    txt(d,(cx+35,445),'最近动态',fill=NAVY,sz=14)
    activities=['09:30  管理员  发起了2026年3月的月度监测',
                '10:15  王五    提交了北京分行存款数据',
                '11:00  李四    确认了上海分行绩效数据',
                '12:00  管理员  发布了2026年2月绩效结果']
    for i,a in enumerate(activities):
        txt(d,(cx+35,475+i*55),a,fill=DGRAY,sz=10)
    img.save(OUT_DIR+'/02_dashboard.png')
    print('02_dashboard.png done')

# ---- 考核体系列表 ----
def draw_system_list():
    img,d=new_img()
    rect(d,[0,0,W,H],fill=BG)
    cx=layout_base(d,active=1)
    txtc(d,cx+580,80,'考核体系管理',fill=NAVY,sz=18)
    txt(d,(cx+20,110),'共 5 个考核体系',fill=DGRAY,sz=11)
    rect(d,[cx+20,135,cx+220,175],fill=WHITE,outline=BORDER,w=1)
    txt(d,(cx+30,150),'搜索体系名称...',fill=(160,160,160),sz=10)
    rect(d,[cx+240,135,cx+290,175],fill=LGRAY,outline=BORDER,w=1)
    txt(d,(cx+248,150),'筛选',fill=DGRAY,sz=10)
    rect(d,[980,135,1110,175],fill=TEAL)
    txtc(d,1045,148,'+ 创建体系',fill=WHITE,sz=11)
    cols=['体系名称','描述','机构数','指标数','状态','创建时间','操作']
    cws=[220,300,80,80,90,150,180]
    x=cx+20; y=195
    table_header(d,x,y,cols,cws)
    rows=[
        ('2026年度分行绩效考核体系','适用于全行12家分行年度考核','12','45','启用','2026-01-15','编辑  查看  删除'),
        ('2026年Q1专项考核','Q1专项业务考核','8','20','启用','2026-01-10','编辑  查看  删除'),
        ('2025年度考核体系','历史考核数据','12','42','禁用','2025-01-01','编辑  查看  删除'),
        ('2025年Q4考核','Q4专项考核','10','18','禁用','2025-10-01','编辑  查看  删除'),
        ('2025年Q3考核','Q3专项考核','10','18','禁用','2025-07-01','编辑  查看  删除'),
    ]
    for ri,row in enumerate(rows):
        yy=226+ri*44; bg=WHITE if ri%2==0 else(248,249,250)
        xx=x
        for ci,(v,cw) in enumerate(zip(row,cws)):
            rect(d,[xx,yy,xx+cw,yy+44],fill=bg,outline=BORDER,w=1)
            if ci==4:
                tc=GREEN if v=='启用' else(150,150,150)
                tag(d,xx+4,yy+12,v,fill=tc)
            elif ci==6:
                txt(d,(xx+4,yy+14),'编辑  查看  删除',fill=LIGHT_BLUE,sz=8)
            else:
                txt(d,(xx+4,yy+14),str(v)[:int(cw/5)],fill=DGRAY,sz=8)
            xx+=cw
    yy=226+5*44+10
    txt(d,(cx+20,yy+5),'共 5 条',fill=DGRAY,sz=9)
    rect(d,[530,yy-5,560,yy+20],fill=TEAL)
    txt(d,(538,yy+2),'1',fill=WHITE,sz=9)
    rect(d,[565,yy-5,595,yy+20],fill=WHITE,outline=BORDER,w=1)
    txt(d,(573,yy+2),'2',fill=DGRAY,sz=9)
    txt(d,(605,yy+5),'下一页',fill=DGRAY,sz=9)
    img.save(OUT_DIR+'/03_system_list.png')
    print('03_system_list.png done')

# ---- 发起监测弹窗 ----
def draw_monitoring_create():
    img,d=new_img()
    rect(d,[0,0,W,H],fill=BG)
    cx=layout_base(d,active=2)
    txtc(d,cx+560,80,'月度考核监测',fill=NAVY,sz=18)
    rect(d,[980,135,1110,175],fill=TEAL)
    txtc(d,1045,148,'+ 发起监测',fill=WHITE,sz=11)
    # 遮罩
    rect(d,[0,0,W,H],fill=(0,0,0,100))
    # 弹窗
    mx,my,mw,mh=300,120,600,530
    rrect(d,[mx,my,mx+mw,my+mh],r=12,fill=WHITE,outline=BORDER,w=1)
    rect(d,[mx,my,mx+mw,my+55],fill=LGRAY,outline=BORDER,w=1)
    txt(d,(mx+25,my+18),'发起月度监测',fill=NAVY,sz=16)
    rect(d,[mx+mw-50,my+15,mx+mw-20,my+40],fill=LGRAY)
    txt(d,(mx+mw-44,my+18),'X',fill=DGRAY,sz=14)
    labels=['选择考核体系 *','年份 *','月份 *','收数截止时间 *','是否需要审批']
    vals=['请选择体系...','2026','3','2026-03-27 18:00','关闭（继承体系设置）']
    for i,(lbl,val) in enumerate(zip(labels,vals)):
        y=my+75+i*70
        txt(d,(mx+25,y),lbl,fill=DGRAY,sz=10)
        rect(d,[mx+25,y+18,mx+25+530,y+60],fill=LGRAY,outline=BORDER,w=1)
        txt(d,(mx+35,y+30),val,fill=DGRAY if i>0 else(160,160,160),sz=11)
    rect(d,[mx+200,my+mh-80,mx+300,my+mh-40],fill=LGRAY,outline=BORDER,w=1)
    txtc(d,mx+250,my+mh-67,'取消',fill=DGRAY,sz=11)
    rect(d,[mx+320,my+mh-80,mx+570,my+mh-40],fill=TEAL)
    txtc(d,mx+445,my+mh-67,'确认发起',fill=WHITE,sz=11)
    img.save(OUT_DIR+'/04_monitoring_create.png')
    print('04_monitoring_create.png done')

# ---- 监测详情页 ----
def draw_monitoring_detail():
    img,d=new_img()
    rect(d,[0,0,W,H],fill=BG)
    cx=layout_base(d,active=2)
    txt(d,(cx+20,75),'< 返回',fill=LIGHT_BLUE,sz=10)
    txtc(d,cx+580,75,'月度监测详情',fill=NAVY,sz=18)
    # 步骤条
    steps=['1.发起','2.收数中','3.数据落库','4.待确认','5.已发布']
    scolors=[GREEN,LIGHT_BLUE,PURPLE,ORANGE,GRAY]
    sx=cx+20
    for i,(s,sc) in enumerate(zip(steps,scolors)):
        sy=115
        if i<3:
            rect(d,[sx,sy,sx+195,sy+50],fill=sc)
            txtc(d,sx+97,sy+15,s,fill=WHITE,sz=11)
        else:
            rect(d,[sx,sy,sx+195,sy+50],fill=LGRAY,outline=BORDER,w=1)
            txtc(d,sx+97,sy+15,s,fill=DGRAY,sz=11)
        if i<4:
            d.polygon([(sx+195,sy+25),(sx+205,sy+15),(sx+205,sy+35)],fill=BORDER)
        sx+=215
    # 信息卡片
    info=[('体系名称','2026年度分行绩效考核体系'),('监测月份','2026年3月'),
          ('当前状态','收数中'),('收数截止','2026-03-27 18:00'),('待填写','8'),('已提交','52')]
    for i,(l,v) in enumerate(info):
        x=cx+20+(i%3)*330; y=180+(i//3)*85
        rrect(d,[x,y,x+310,y+70],r=6,fill=WHITE,outline=BORDER,w=1)
        txt(d,(x+12,y+10),l,fill=DGRAY,sz=10)
        if l=='当前状态': tag(d,x+12,y+35,'收数中',LIGHT_BLUE)
        else: txt(d,(x+12,y+35),v,fill=NAVY,sz=12)
    # Tabs
    tabs=['数据收集','落库进度','确认情况','关联文件']
    for i,tab in enumerate(tabs):
        x=cx+20+i*150; y=370
        bc=NAVY if i==0 else DGRAY
        txt(d,(x,y),tab,fill=bc,sz=11)
        if i==0: rect(d,[x,y+22,x+60,y+25],fill=NAVY)
    # 数据收集表格
    y=410
    cols=['机构','指标','收数人','实际值','状态','提交时间']
    cws=[150,200,150,120,100,180]
    xx=x=cx+20
    rect(d,[x,y,x+sum(cws),y+36],fill=LGRAY,outline=BORDER,w=1)
    for h,cw in zip(cols,cws):
        txt(d,(xx+4,y+10),h,fill=DGRAY,sz=9); xx+=cw
    y+=36
    data=[('北京分行','日均存款余额','王五/EMP005','7.85','已提交','03-27 10:30'),
          ('北京分行','存款增长率','王五/EMP005','1.10','已提交','03-27 10:30'),
          ('上海分行','不良贷款率','李四/EMP002','-','待填写','-'),
          ('深圳分行','拨备覆盖率','赵六/EMP003','16.50','已提交','03-27 11:00')]
    for ri,row in enumerate(data):
        yy=y+ri*50; bg=WHITE if ri%2==0 else(248,249,250)
        xx=x
        for ci,(v,cw) in enumerate(zip(row,cws)):
            rect(d,[xx,yy,xx+cw,yy+50],fill=bg,outline=BORDER,w=1)
            if ci==4:
                tc=GREEN if v=='已提交' else(150,150,150)
                tag(d,xx+4,yy+17,v,fill=tc)
            else:
                txt(d,(xx+4,yy+17),str(v)[:int(cw/5)],fill=DGRAY,sz=8)
            xx+=cw
    rect(d,[920,y+10,1100,y+45],fill=TEAL)
    txtc(d,1010,y+20,'整体上传',fill=WHITE,sz=10)
    img.save(OUT_DIR+'/05_monitoring_detail.png')
    print('05_monitoring_detail.png done')

# ---- 数据填写页 ----
def draw_collect():
    img,d=new_img()
    rect(d,[0,0,W,H],fill=BG)
    rect(d,[0,0,W,60],fill=NAVY)
    txt(d,(30,20),'机构绩效管理系统',fill=WHITE,sz=16)
    txt(d,(700,22),'收数人：王五  |  退出',fill=(180,200,220),sz=10)
    rect(d,[0,60,200,H],fill=WHITE,outline=BORDER,w=1)
    for i,t in enumerate(['首页仪表盘','数据填写','绩效报表']):
        y=80+i*50; bg=TEAL if i==1 else None
        fc=WHITE if i==1 else DGRAY
        if bg: rect(d,[0,y,200,y+44],fill=bg)
        txt(d,(20,y+14),t,fill=fc,sz=11)
    cx=220
    txtc(d,cx+580,80,'数据填写',fill=NAVY,sz=18)
    txt(d,(cx+20,110),'您有 2 个待填写的收数任务',fill=DGRAY,sz=11)
    # 机构分组
    institutions=[('北京分行','2026年度分行绩效考核体系','2026年3月','截止：03-27 18:00'),
                   ('杭州分行','2026年度分行绩效考核体系','2026年3月','截止：03-27 18:00')]
    tasks_data=[
        [('日均存款余额','亿元','8.33','7.85'),('存款增长率','%','1.25','1.10')],
        [('日均存款余额','亿元','8.33',''),('存款增长率','%','1.25','')],
    ]
    y=145
    for ii,(inst,sys,mon,dl) in enumerate(institutions):
        rrect(d,[cx+20,y,cx+20+1100,y+90],r=8,fill=WHITE,outline=BORDER,w=1)
        rect(d,[cx+20,y,cx+20+1100,y+4],fill=TEAL)
        txt(d,(cx+35,y+14),inst,fill=NAVY,sz=13)
        txt(d,(cx+35,y+38),sys+'  |  '+mon,fill=DGRAY,sz=10)
        txt(d,(cx+35,y+62),dl,fill=ORANGE,sz=10)
        txt(d,(950,y+38),'下载Excel',fill=LIGHT_BLUE,sz=10)
        txt(d,(1050,y+38),'上传Excel',fill=LIGHT_BLUE,sz=10)
        y+=95
        hdrs=['指标名称','单位','进度目标','全年目标','实际值']
        cws=[200,100,150,150,150,250]
        xx=cx+20
        rect(d,[cx+20,y,cx+20+sum(cws),y+32],fill=LGRAY,outline=BORDER,w=1)
        for h,cw in zip(hdrs,cws):
            txt(d,(xx+4,y+8),h,fill=DGRAY,sz=9); xx+=cw
        y+=32
        for ti,task in enumerate(tasks_data[ii]):
            bg=WHITE if ti%2==0 else(248,249,250)
            xx=cx+20
            for vi,(v,cw) in enumerate(zip(task,cws)):
                rect(d,[xx,y,xx+cw,y+40],fill=bg,outline=BORDER,w=1)
                if vi==4:
                    rect(d,[xx+4,y+6,xx+cw-10,y+34],fill=LGRAY,outline=BORDER,w=1)
                    txt(d,(xx+8,y+16),v,fill=DGRAY,sz=9)
                else:
                    txt(d,(xx+4,y+14),str(v),fill=DGRAY,sz=9)
                xx+=cw
            y+=40
        submitted=all(t[3]!='' for t in tasks_data[ii])
        btn_bg=TEAL if submitted else GRAY
        rect(d,[cx+20+950,y+5,cx+20+1100,y+40],fill=btn_bg)
        txtc(d,cx+20+1025,y+16,'提交数据',fill=WHITE,sz=10)
        y+=60
    img.save(OUT_DIR+'/06_collect.png')
    print('06_collect.png done')

# ---- 绩效报表页 ----
def draw_report():
    img,d=new_img()
    rect(d,[0,0,W,H],fill=BG)
    cx=layout_base(d,active=3)
    txtc(d,cx+580,80,'绩效结果展示',fill=NAVY,sz=18)
    filters=[('2026年度分行绩效考核体系',cx+20),
             ('2026年',cx+230),('3月',cx+390),
             ('全部机构',cx+500),('全部分组',cx+660)]
    for txt_s,x in filters:
        rect(d,[x,115,x+195,155],fill=WHITE,outline=BORDER,w=1)
        txt(d,(x+10,127),txt_s,fill=DGRAY,sz=9)
    rect(d,[cx+850,115,cx+900,155],fill=LGRAY,outline=BORDER,w=1)
    txtc(d,cx+875,128,'重置',fill=DGRAY,sz=9)
    tabs=[('数据表格',cx+20),('可视化图表',cx+170),('总览对比',cx+320)]
    for i,(tab,x) in enumerate(tabs):
        bc=NAVY if i==1 else DGRAY
        txt(d,(x,175),tab,fill=bc,sz=11)
        if i==1: rect(d,[x,y=175,x+60,y+25],fill=NAVY)
    # 柱状图
    y=215
    rrect(d,[cx+20,y,cx+20+540,y+360],r=8,fill=WHITE,outline=BORDER,w=1)
    txt(d,(cx+35,y+12),'各机构总得分排名',fill=NAVY,sz=11)
    bars=[('北京',92.3,TEAL),('上海',88.9,LIGHT_BLUE),('深圳',85.6,PURPLE),('杭州',82.1,ORANGE),('成都',78.4,GRAY)]
    bx=cx+35; bh=260; bmax=100
    for nm,sc,col in bars:
        bh2=int(250*sc/bmax)
        rect(d,[bx,y+30+250-bh2,bx+60,y+280],fill=col)
        txt(d,(bx+10,y+280-bh2-20),f'{sc}',fill=DGRAY,sz=8)
        txt(d,(bx+10,y+285),nm,fill=DGRAY,sz=8)
        bx+=85
    # 雷达图
    rrect(d,[cx+580,y,cx+580+540,y+360],r=8,fill=WHITE,outline=BORDER,w=1)
    txt(d,(cx+595,y+12),'维度得分对比（雷达图）',fill=NAVY,sz=11)
    rcx,rcy=cx+850,y+190; rr=110
    for angle in range(0,360,60):
        a=math.radians(angle)
        x1=rcx+int(rr*math.cos(a)); y1=rcy+int(rr*math.sin(a))
        d.line([(rcx,rcy),(x1,y1)],fill=GRAY,w=1)
    d.ellipse([rcx-rr,rcy-rr,rcx+rr,rcy+rr],outline=GRAY,w=1)
    pts_bj=[]
    for i,v in enumerate([0.92,0.88,0.85,0.90]):
        a=math.radians(i*90); x1=rcx+int(rr*v*math.cos(a)); y1=rcy+int(rr*v*math.sin(a)); pts_bj.append((x1,y1))
    d.polygon(pts_bj,fill=(4,138,129,80),outline=TEAL,w=2)
    pts_sh=[]
    for i,v in enumerate([0.88,0.85,0.82,0.88]):
        a=math.radians(i*90); x1=rcx+int(rr*v*math.cos(a)); y1=rcy+int(rr*v*math.sin(a)); pts_sh.append((x1,y1))
    d.polygon(pts_sh,fill=(24,144,255,80),outline=LIGHT_BLUE,w=2)
    txt(d,(cx+595,y+320),'■ 北京分行',fill=TEAL,sz=9)
    txt(d,(cx+710,y+320),'■ 上海分行',fill=LIGHT_BLUE,sz=9)
    txt(d,(cx+825,y+320),'■ 深圳分行',fill=PURPLE,sz=9)
    # 进度条
    y2=y+375
    rrect(d,[cx+20,y2,cx+20+1100,y2+165],r=8,fill=WHITE,outline=BORDER,w=1)
    txt(d,(cx+35,y2+12),'关键指标完成率对比',fill=NAVY,sz=11)
    for i,(nm,pct,col) in enumerate([('日均存款余额',0.942,TEAL),('存款增长率',0.880,LIGHT_BLUE),
                                      ('不良贷款率',0.960,GREEN),('拨备覆盖率',0.920,PURPLE)]):
        iy=y2+42+i*30
        txt(d,(cx+35,iy),nm,fill=DGRAY,sz=9)
        pbar(d,cx+180,iy+2,600,16,int(pct*100),col)
        txt(d,(cx+790,iy),f'{int(pct*100)}%',fill=DGRAY,sz=9)
    img.save(OUT_DIR+'/07_report.png')
    print('07_report.png done')

# ---- 通知中心 ----
def draw_notifications():
    img,d=new_img()
    rect(d,[0,0,W,H],fill=BG)
    rect(d,[0,0,W,60],fill=NAVY)
    txt(d,(30,20),'机构绩效管理系统',fill=WHITE,sz=16)
    badge(d,660,20,'3',RED)
    txt(d,(700,22),'管理员：张三  |  退出',fill=(180,200,220),sz=10)
    rect(d,[0,60,200,H],fill=WHITE,outline=BORDER,w=1)
    for i,t in enumerate(['首页仪表盘','考核体系管理','月度考核监测','绩效结果展示','通知中心']):
        y=80+i*50; bg=TEAL if i==4 else None
        fc=WHITE if i==4 else DGRAY
        if bg: rect(d,[0,y,200,y+44],fill=bg)
        txt(d,(20,y+14),t,fill=fc,sz=11)
    cx=220
    txtc(d,cx+580,80,'通知中心',fill=NAVY,sz=18)
    txt(d,(cx+20,110),'共 15 条通知，3 条未读',fill=DGRAY,sz=11)
    for i,(tab,x) in enumerate([('全部',cx+20),('未读',cx+100)]):
        bc=NAVY if i==1 else DGRAY
        txt(d,(x,145),tab,fill=bc,sz=11)
        if i==1: rect(d,[x,y=145,x+30,y+25],fill=NAVY)
        badge(d,x+35,143,'3',RED)
    txt(d,(cx+870,145),'全部标为已读',fill=LIGHT_BLUE,sz=10)
    notifs=[
        ('📊','【收数任务】您有新的数据待填写','体系：2026年度分行绩效考核体系，月份：2026年3月，请于03月27日18:00前完成。','3分钟前',True,'task'),
        ('🔔','【截止提醒】收数即将截止','您的北京分行存款数据还未提交，距离截止还有30分钟，请尽快填写。','30分钟前',True,'reminder'),
        ('✅','【确认通知】请确认本机构绩效数据','2026年2月绩效数据已生成，请确认北京分行的绩效数据。','2小时前',False,'confirm'),
        ('📢','【发布通知】2026年2月绩效结果已发布','管理员已发布2026年2月的绩效结果，数据现已公开，请查看。','1天前',False,'publish'),
    ]
    for i,(icon,title,content,time,unread,typ) in enumerate(notifs):
        y=185+i*120
        rrect(d,[cx+20,y,cx+20+1100,y+105],r=8,fill=WHITE,outline=BORDER,w=1)
        if unread: rect(d,[cx+20,y,cx+25,y+105],fill=LIGHT_BLUE)
        txt(d,(cx+40,y+15),icon,fill=DGRAY,sz=20)
        txt(d,(cx+75,y+12),title,fill=NAVY,sz=11)
        txt(d,(cx+75,y+38),content,fill=DGRAY,sz=9)
        txt(d,(cx+75,y+78),time,fill=(160,160,160),sz=9)
        if unread: txt(d,(cx+920,y+78),'标为已读',fill=LIGHT_BLUE,sz=9)
        txt(d,(cx+1020,y+78),'查看详情 >',fill=LIGHT_BLUE,sz=9)
    yy=185+4*120+20
    txt(d,(cx+20,yy),'共 15 条',fill=DGRAY,sz=9)
    for i in range(2):
        x=500+i*40; bg=NAVY if i==0 else WHITE; fc=WHITE if i==0 else DGRAY
        rect(d,[x,yy-5,x+30,yy+20],fill=bg,outline=BORDER,w=1)
        txt(d,(x+8,yy+2),str(i+1),fill=fc,sz=9)
    txt(d,(570,yy),'下一页',fill=DGRAY,sz=9)
    img.save(OUT_DIR+'/08_notifications.png')
    print('08_notifications.png done')

# Run all
draw_login()
draw_dashboard()
draw_system_list()
draw_monitoring_create()
draw_monitoring_detail()
draw_collect()
draw_report()
draw_notifications()
print('\nAll mockups generated in', OUT_DIR)
