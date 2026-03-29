#!/usr/bin/env python3
"""Generate SVG wireframe mockups for the page design document."""
import os, math

OUT_DIR = '/Users/zhaoyu/Desktop/UI_Mockups'
os.makedirs(OUT_DIR, exist_ok=True)

def svg_header(w, h):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">\n'

def rect(x, y, w, h, fill=None, stroke='#cccccc', sw=1, rx=0, ry=0, opacity=1):
    s = f'<rect x="{x}" y="{y}" width="{w}" height="{h}"'
    if fill: s += f' fill="{fill}"'
    else: s += ' fill="none"'
    s += f' stroke="{stroke}" stroke-width="{sw}"'
    if rx or ry: s += f' rx="{rx}" ry="{ry}"'
    if opacity < 1: s += f' opacity="{opacity}"'
    return s + '/>\n'

def text(x, y, s, fill='#666666', sz=12, bold=False, anchor='start'):
    w = 'font-weight="bold"' if bold else ''
    return f'<text x="{x}" y="{y}" font-family="Helvetica,Arial" font-size="{sz}" fill="{fill}" {w} text-anchor="{anchor}">{s}</text>\n'

def line(x1, y1, x2, y2, stroke='#cccccc', sw=1):
    return f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{stroke}" stroke-width="{sw}"/>\n'

def circle(cx, cy, r, fill='#cccccc', stroke='none'):
    return f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{fill}" stroke="{stroke}"/>\n'

def polyline(points, fill='none', stroke='#cccccc', sw=1, opacity=0.3):
    pts = ' '.join([f'{x},{y}' for x,y in points])
    r,g,b = int(fill[1:3],16), int(fill[3:5],16), int(fill[5:7],16)
    a = int(opacity*255)
    return f'<polygon points="{pts}" fill="rgba({r},{g},{b},{opacity})" stroke="{stroke}" stroke-width="{sw}"/>\n'

def footer():
    return '</svg>\n'

def navy(): return '#2E4057'
def teal(): return '#048A81'
def blue(): return '#1890FF'
def orange(): return '#FA8C16'
def green(): return '#52C41A'
def gray(): return '#999999'
def lgray(): return '#F5F5F5'
def white(): return '#FFFFFF'
def dgray(): return '#666666'

# ============ Page 1: 登录页 ============
def page_login():
    c = svg_header(1200, 800)
    c += rect(0, 0, 1200, 800, fill='#F8F9FA')
    # 左侧
    c += rect(0, 0, 500, 800, fill=navy())
    c += text(250, 200, '机构绩效管理系统', '#FFFFFF', 28, True, 'middle')
    c += text(250, 235, 'Institution Performance', '#9AB2C8', 16, False, 'middle')
    c += text(250, 262, 'Management System', '#9AB2C8', 16, False, 'middle')
    c += text(250, 350, '数字化赋能分行绩效考核', '#9AB2C8', 13, False, 'middle')
    c += circle(420, 555, 70, fill='#3C5069', stroke='none')
    c += circle(105, 620, 75, fill='#3C5069', stroke='none')
    # 右侧卡片
    cx, cy, cw, ch = 700, 140, 420, 520
    c += f'<rect x="{cx}" y="{cy}" width="{cw}" height="{ch}" rx="12" fill="{white()}" stroke="#E8E8E8" stroke-width="1"/>\n'
    c += text(cx+40, cy+50, '用户登录', navy(), 22, True)
    c += text(cx+40, cy+85, '请输入您的账号信息', dgray(), 12)
    c += text(cx+40, cy+125, '用户名（工号）', dgray(), 11)
    c += rect(cx+40, cy+142, 320, 42, fill=lgray(), stroke='#E0E0E0', rx=4)
    c += text(cx+52, cy+162, '请输入工号', '#AAAAAA', 12)
    c += text(cx+40, cy+208, '密码', dgray(), 11)
    c += rect(cx+40, cy+225, 320, 42, fill=lgray(), stroke='#E0E0E0', rx=4)
    c += text(cx+52, cy+245, '••••••••', '#AAAAAA', 12)
    c += rect(cx+40, cy+300, 320, 50, fill=teal(), rx=4)
    c += text(cx+200, cy+332, '登 录', white(), 14, True, 'middle')
    c += text(cx+cw//2, cy+ch-30, '机构绩效管理系统 V1.0', '#CCCCCC', 10, False, 'middle')
    c += footer()
    with open(f'{OUT_DIR}/01_login.svg', 'w') as f: f.write(c)
    print('01_login.svg done')

# ============ Page 2: Dashboard ============
def page_dashboard():
    c = svg_header(1200, 800)
    c += rect(0, 0, 1200, 800, fill='#F8F9FA')
    # Header
    c += rect(0, 0, 1200, 60, fill=navy())
    c += text(30, 32, '机构绩效管理系统', white(), 16, True)
    c += rect(640, 22, 22, 20, fill='#F5222D')
    c += text(651, 36, '5', white(), 10, True)
    c += text(680, 32, '管理员：张三  |  退出', '#B4CCE8', 11)
    # Sidebar
    c += rect(0, 60, 200, 740, fill=white(), stroke='#E8E8E8')
    menus = [('🏠 首页仪表盘', teal()), ('📋 考核体系管理', dgray()),
             ('📊 月度考核监测', dgray()), ('📈 绩效结果展示', dgray()), ('🔔 通知中心', dgray())]
    for i, (m, col) in enumerate(menus):
        y = 80 + i*50
        if i == 0: c += rect(0, y, 200, 44, fill=col)
        c += text(20, y+26, m, col if i else white(), 12)
    # Content
    cx = 220
    c += text(cx+580, 88, '首页仪表盘', navy(), 20, True, 'middle')
    c += text(cx+20, 120, '欢迎回来，张三', dgray(), 12)
    stats = [('进行中监测', '3', teal()), ('待确认', '1', orange()),
             ('已发布', '12', green()), ('我的待办', '5', '#722ED1')]
    for i, (t, n, col) in enumerate(stats):
        x = cx+20+i*238; y = 145
        c += f'<rect x="{x}" y="{y}" width="218" height="110" rx="8" fill="{white()}" stroke="#E8E8E8" stroke-width="1"/>\n'
        c += rect(x, y, 218, 4, fill=col)
        c += text(x+15, y+30, t, dgray(), 12)
        c += text(x+15, y+62, n, col, 32, True)
        c += text(x+90, y+72, '个', dgray(), 12)
    c += text(cx+20, 290, '快捷入口', navy(), 14, True)
    btns = [('发起月度监测', teal()), ('填写绩效数据', blue()),
            ('查看绩效报表', '#722ED1'), ('通知中心', orange())]
    for i, (b, bc) in enumerate(btns):
        x = cx+20+i*200; y = 315
        c += f'<rect x="{x}" y="{y}" width="180" height="55" rx="6" fill="{bc}"/>\n'
        c += text(x+90, y+32, b, white(), 12, True, 'middle')
    c += f'<rect x="{cx+20}" y="430" width="1080" height="300" rx="8" fill="{white()}" stroke="#E8E8E8"/>\n'
    c += text(cx+35, 452, '最近动态', navy(), 14, True)
    for i, a in enumerate(['09:30  管理员  发起了2026年3月的月度监测',
                            '10:15  王五    提交了北京分行存款数据',
                            '11:00  李四    确认了上海分行绩效数据',
                            '12:00  管理员  发布了2026年2月绩效结果']):
        c += text(cx+35, 485+i*58, a, dgray(), 11)
    c += footer()
    with open(f'{OUT_DIR}/02_dashboard.svg', 'w') as f: f.write(c)
    print('02_dashboard.svg done')

# ============ Page 3: 体系列表 ============
def page_system_list():
    c = svg_header(1200, 800)
    c += rect(0, 0, 1200, 800, fill='#F8F9FA')
    c += rect(0, 0, 1200, 60, fill=navy())
    c += text(30, 32, '机构绩效管理系统', white(), 16, True)
    c += rect(0, 60, 200, 740, fill=white(), stroke='#E8E8E8')
    menus = [('🏠 首页仪表盘', dgray()), ('📋 考核体系管理', teal()),
             ('📊 月度考核监测', dgray()), ('📈 绩效结果展示', dgray()), ('🔔 通知中心', dgray())]
    for i, (m, col) in enumerate(menus):
        y = 80 + i*50
        if i == 1: c += rect(0, y, 200, 44, fill=col)
        c += text(20, y+26, m, col if i!=1 else white(), 12)
    cx = 220
    c += text(cx+570, 88, '考核体系管理', navy(), 20, True, 'middle')
    c += text(cx+20, 120, '共 5 个考核体系', dgray(), 12)
    c += rect(cx+20, 138, 200, 38, fill=white(), stroke='#E0E0E0', rx=4)
    c += text(cx+30, 159, '搜索体系名称...', '#AAAAAA', 11)
    c += rect(cx+240, 138, 60, 38, fill=lgray(), stroke='#E0E0E0', rx=4)
    c += text(cx+248, 159, '筛选', dgray(), 11)
    c += rect(980, 138, 120, 38, fill=teal(), rx=4)
    c += text(1040, 159, '+ 创建体系', white(), 12, True, 'middle')
    # Table
    cols = ['体系名称', '描述', '机构数', '指标数', '状态', '创建时间', '操作']
    cws = [220, 300, 80, 80, 90, 150, 180]
    x = cx+20; y = 195
    c += rect(x, y, sum(cws), 36, fill=navy())
    xx = x
    for h, cw in zip(cols, cws):
        c += text(xx+6, y+24, h, white(), 10, True)
        xx += cw
    y += 36
    rows = [
        ('2026年度分行绩效考核体系', '适用于全行12家分行年度考核', '12', '45', '启用', '2026-01-15', '编辑  查看  删除'),
        ('2026年Q1专项考核', 'Q1专项业务考核', '8', '20', '启用', '2026-01-10', '编辑  查看  删除'),
        ('2025年度考核体系', '历史考核数据', '12', '42', '禁用', '2025-01-01', '编辑  查看  删除'),
        ('2025年Q4考核', 'Q4专项考核', '10', '18', '禁用', '2025-10-01', '编辑  查看  删除'),
        ('2025年Q3考核', 'Q3专项考核', '10', '18', '禁用', '2025-07-01', '编辑  查看  删除'),
    ]
    for ri, row in enumerate(rows):
        yy = y+ri*44; bg = white() if ri%2==0 else '#F8F9FA'
        xx = x
        for ci, (v, cw) in enumerate(zip(row, cws)):
            c += rect(xx, yy, cw, 44, fill=bg, stroke='#E8E8E8')
            if ci == 4:
                tc = green() if v=='启用' else gray()
                c += f'<rect x="{xx+4}" y="{yy+12}" width="36" height="20" rx="3" fill="{tc}"/>\n'
                c += text(xx+22, yy+27, v, white(), 9, True, 'middle')
            elif ci == 6:
                c += text(xx+6, yy+27, v, blue(), 9)
            else:
                c += text(xx+6, yy+27, str(v)[:int(cw/6)], dgray(), 9)
            xx += cw
    yy = y+5*44+20
    c += text(cx+20, yy, '共 5 条', dgray(), 10)
    c += rect(530, yy-8, 30, 24, fill=teal())
    c += text(545, yy+8, '1', white(), 10, True, 'middle')
    c += rect(565, yy-8, 30, 24, fill=white(), stroke='#E0E0E0')
    c += text(580, yy+8, '2', dgray(), 10, True, 'middle')
    c += text(605, yy, '下一页', dgray(), 10)
    c += footer()
    with open(f'{OUT_DIR}/03_system_list.svg', 'w') as f: f.write(c)
    print('03_system_list.svg done')

# ============ Page 4: 发起监测弹窗 ============
def page_monitoring_create():
    c = svg_header(1200, 800)
    c += rect(0, 0, 1200, 800, fill='#F8F9FA')
    c += rect(0, 0, 1200, 60, fill=navy())
    c += text(30, 32, '机构绩效管理系统', white(), 16, True)
    c += rect(0, 60, 200, 740, fill=white(), stroke='#E8E8E8')
    for i, t in enumerate(['首页仪表盘', '考核体系管理', '月度考核监测', '绩效结果展示']):
        y = 80+i*50; col = teal() if i==2 else dgray()
        if i==2: c += rect(0, y, 200, 44, fill=col)
        c += text(20, y+26, t, col if i!=2 else white(), 12)
    cx = 220
    c += text(cx+560, 88, '月度考核监测', navy(), 20, True, 'middle')
    c += rect(980, 138, 120, 38, fill=teal(), rx=4)
    c += text(1040, 159, '+ 发起监测', white(), 12, True, 'middle')
    # Overlay
    c += rect(0, 0, 1200, 800, fill='rgba(0,0,0,0.5)')
    # Modal
    mx, my, mw, mh = 300, 100, 600, 540
    c += f'<rect x="{mx}" y="{my}" width="{mw}" height="{mh}" rx="12" fill="{white()}" stroke="#E8E8E8" stroke-width="1"/>\n'
    c += rect(mx, my, mw, 55, fill=lgray(), stroke='#E8E8E8')
    c += text(mx+30, my+30, '发起月度监测', navy(), 18, True)
    c += rect(mx+mw-50, my+15, 30, 26, fill='#EEEEEE', rx=4)
    c += text(mx+mw-40, my+33, '×', '#888888', 18, True)
    labels = ['选择考核体系 *', '年份 *', '月份 *', '收数截止时间 *', '是否需要审批']
    vals = ['请选择体系...', '2026', '3', '2026-03-27 18:00', '关闭（继承体系设置）']
    for i, (lbl, val) in enumerate(zip(labels, vals)):
        y = my+70+i*72
        c += text(mx+30, y, lbl, dgray(), 11)
        c += rect(mx+30, y+18, 540, 42, fill=lgray(), stroke='#E0E0E0', rx=4)
        c += text(mx+42, y+38, val, '#AAAAAA' if i==0 else dgray(), 12)
    c += rect(mx+200, my+mh-75, 100, 42, fill=lgray(), stroke='#E0E0E0', rx=4)
    c += text(mx+250, my+mh-47, '取消', dgray(), 12, True, 'middle')
    c += rect(mx+320, my+mh-75, 250, 42, fill=teal(), rx=4)
    c += text(mx+445, my+mh-47, '确认发起', white(), 12, True, 'middle')
    c += footer()
    with open(f'{OUT_DIR}/04_monitoring_create.svg', 'w') as f: f.write(c)
    print('04_monitoring_create.svg done')

# ============ Page 5: 监测详情 ============
def page_monitoring_detail():
    c = svg_header(1200, 800)
    c += rect(0, 0, 1200, 800, fill='#F8F9FA')
    c += rect(0, 0, 1200, 60, fill=navy())
    c += text(30, 32, '机构绩效管理系统', white(), 16, True)
    c += rect(0, 60, 200, 740, fill=white(), stroke='#E8E8E8')
    for i, t in enumerate(['首页仪表盘', '考核体系管理', '月度考核监测', '绩效结果展示']):
        y = 80+i*50; col = teal() if i==2 else dgray()
        if i==2: c += rect(0, y, 200, 44, fill=col)
        c += text(20, y+26, t, col if i!=2 else white(), 12)
    cx = 220
    c += text(cx+20, 82, '← 返回', blue(), 11)
    c += text(cx+80, 82, '月度监测详情', navy(), 20, True)
    # Steps
    steps = ['1.发起', '2.收数中', '3.数据落库', '4.待确认', '5.已发布']
    scolors = [green(), blue(), '#722ED1', orange(), gray()]
    sx = cx+20
    for i, (s, sc) in enumerate(zip(steps, scolors)):
        sy = 115
        if i < 3:
            c += f'<rect x="{sx}" y="{sy}" width="195" height="50" rx="6" fill="{sc}"/>\n'
        else:
            c += f'<rect x="{sx}" y="{sy}" width="195" height="50" rx="6" fill="{lgray()}" stroke="#E0E0E0"/>\n'
        c += text(sx+97, sy+32, s, white() if i<3 else dgray(), 12, True, 'middle')
        if i < 4:
            pts = f'{sx+195},{sy+25} {sx+208},{sy+12} {sx+208},{sy+38}'
            c += f'<polygon points="{pts}" fill="#E0E0E0"/>\n'
        sx += 215
    # Info cards
    info = [('体系名称','2026年度分行绩效考核体系'),('监测月份','2026年3月'),
            ('当前状态','收数中'),('收数截止','2026-03-27 18:00'),('待填写','8'),('已提交','52')]
    for i, (l, v) in enumerate(info):
        x = cx+20+(i%3)*330; y = 180+(i//3)*85
        c += f'<rect x="{x}" y="{y}" width="310" height="70" rx="6" fill="{white()}" stroke="#E8E8E8"/>\n'
        c += text(x+12, y+18, l, dgray(), 11)
        if l == '当前状态':
            c += f'<rect x="{x+12}" y="{y+40}" width="50" height="20" rx="3" fill="{blue()}"/>\n'
            c += text(x+37, y+55, '收数中', white(), 9, True, 'middle')
        else:
            c += text(x+12, y+55, v, navy(), 13, True)
    # Tabs
    tabs = ['数据收集', '落库进度', '确认情况', '关联文件']
    for i, tab in enumerate(tabs):
        x = cx+20+i*150; y = 375
        c += text(x, y, tab, navy() if i==0 else dgray(), 12)
        if i==0: c += rect(x, y+22, 55, 3, fill=navy())
    # Table
    y = 415
    cols = ['机构', '指标', '收数人', '实际值', '状态', '提交时间']
    cws = [150, 200, 150, 120, 100, 180]
    xx = cx+20
    c += rect(cx+20, y, sum(cws), 36, fill=lgray(), stroke='#E8E8E8')
    for h, cw in zip(cols, cws):
        c += text(xx+4, y+24, h, dgray(), 10)
        xx += cw
    y += 36
    data = [('北京分行','日均存款余额','王五/EMP005','7.85','已提交','03-27 10:30'),
            ('北京分行','存款增长率','王五/EMP005','1.10','已提交','03-27 10:30'),
            ('上海分行','不良贷款率','李四/EMP002','-','待填写','-'),
            ('深圳分行','拨备覆盖率','赵六/EMP003','16.50','已提交','03-27 11:00')]
    for ri, row in enumerate(data):
        yy = y+ri*50; bg = white() if ri%2==0 else '#F8F9FA'
        xx = cx+20
        for ci, (v, cw) in enumerate(zip(row, cws)):
            c += rect(xx, yy, cw, 50, fill=bg, stroke='#E8E8E8')
            if ci == 4:
                tc = green() if v=='已提交' else gray()
                c += f'<rect x="{xx+4}" y="{yy+16}" width="44" height="18" rx="3" fill="{tc}"/>\n'
                c += text(xx+26, yy+29, v, white(), 9, True, 'middle')
            else:
                c += text(xx+6, yy+28, str(v)[:int(cw/5)], dgray(), 9)
            xx += cw
    c += rect(920, y+10, 180, 36, fill=teal(), rx=4)
    c += text(1010, y+32, '整体上传', white(), 11, True, 'middle')
    c += footer()
    with open(f'{OUT_DIR}/05_monitoring_detail.svg', 'w') as f: f.write(c)
    print('05_monitoring_detail.svg done')

# ============ Page 6: 数据填写 ============
def page_collect():
    c = svg_header(1200, 800)
    c += rect(0, 0, 1200, 800, fill='#F8F9FA')
    c += rect(0, 0, 1200, 60, fill=navy())
    c += text(30, 32, '机构绩效管理系统', white(), 16, True)
    c += text(680, 32, '收数人：王五  |  退出', '#B4CCE8', 11)
    c += rect(0, 60, 200, 740, fill=white(), stroke='#E8E8E8')
    for i, t in enumerate(['首页仪表盘', '数据填写', '绩效报表']):
        y = 80+i*50; col = teal() if i==1 else dgray()
        if i==1: c += rect(0, y, 200, 44, fill=col)
        c += text(20, y+26, t, col if i!=1 else white(), 12)
    cx = 220
    c += text(cx+560, 88, '数据填写', navy(), 20, True, 'middle')
    c += text(cx+20, 120, '您有 2 个待填写的收数任务', dgray(), 12)
    institutions = [('北京分行','2026年度分行绩效考核体系','2026年3月','截止：03-27 18:00'),
                    ('杭州分行','2026年度分行绩效考核体系','2026年3月','截止：03-27 18:00')]
    tasks = [
        [('日均存款余额','亿元','8.33','7.85'),('存款增长率','%','1.25','1.10')],
        [('日均存款余额','亿元','8.33',''),('存款增长率','%','1.25','')],
    ]
    y = 145
    for ii, (inst, sys, mon, dl) in enumerate(institutions):
        c += f'<rect x="{cx+20}" y="{y}" width="1100" height="90" rx="8" fill="{white()}" stroke="#E8E8E8"/>\n'
        c += rect(cx+20, y, 1100, 4, fill=teal())
        c += text(cx+35, y+22, inst, navy(), 14, True)
        c += text(cx+35, y+46, sys+'  |  '+mon, dgray(), 11)
        c += text(cx+35, y+68, dl, orange(), 11)
        c += text(950, y+46, '下载Excel', blue(), 11)
        c += text(1060, y+46, '上传Excel', blue(), 11)
        y += 95
        hdrs = ['指标名称','单位','进度目标','全年目标','实际值']
        cws = [200,100,150,150,150,250]
        xx = cx+20
        c += rect(cx+20, y, sum(cws), 32, fill=lgray(), stroke='#E8E8E8')
        for h, cw in zip(hdrs, cws):
            c += text(xx+4, y+20, h, dgray(), 10); xx += cw
        y += 32
        for ti, task in enumerate(tasks[ii]):
            bg = white() if ti%2==0 else '#F8F9FA'
            xx = cx+20
            for vi, (v, cw) in enumerate(zip(task, cws)):
                c += rect(xx, y, cw, 40, fill=bg, stroke='#E8E8E8')
                if vi == 4:
                    c += rect(xx+4, y+6, cw-15, 28, fill=lgray(), stroke='#E0E0E0', rx=3)
                    c += text(xx+10, y+23, v, dgray(), 10)
                else:
                    c += text(xx+6, y+23, str(v), dgray(), 10)
                xx += cw
            y += 40
        submitted = all(t[3]!='' for t in tasks[ii])
        bc = teal() if submitted else gray()
        c += f'<rect x="{cx+20+950}" y="{y+5}" width="130" height="35" rx="4" fill="{bc}"/>\n'
        c += text(cx+20+1015, y+28, '提交数据', white(), 11, True, 'middle')
        y += 60
    c += footer()
    with open(f'{OUT_DIR}/06_collect.svg', 'w') as f: f.write(c)
    print('06_collect.svg done')

# ============ Page 7: 绩效报表 ============
def page_report():
    c = svg_header(1200, 800)
    c += rect(0, 0, 1200, 800, fill='#F8F9FA')
    c += rect(0, 0, 1200, 60, fill=navy())
    c += text(30, 32, '机构绩效管理系统', white(), 16, True)
    c += rect(0, 60, 200, 740, fill=white(), stroke='#E8E8E8')
    for i, t in enumerate(['首页仪表盘', '考核体系管理', '月度考核监测', '绩效结果展示']):
        y = 80+i*50; col = teal() if i==3 else dgray()
        if i==3: c += rect(0, y, 200, 44, fill=col)
        c += text(20, y+26, t, col if i!=3 else white(), 12)
    cx = 220
    c += text(cx+560, 88, '绩效结果展示', navy(), 20, True, 'middle')
    filters = ['2026年度分行绩效考核体系','2026年','3月','全部机构','全部分组']
    for i, (f, x) in enumerate(zip(filters, [cx+20,cx+230,cx+390,cx+500,cx+660])):
        c += rect(x, 118, 195, 38, fill=white(), stroke='#E0E0E0', rx=4)
        c += text(x+10, 138, f, dgray(), 10)
    c += rect(cx+860, 118, 55, 38, fill=lgray(), stroke='#E0E0E0', rx=4)
    c += text(cx+887, 138, '重置', dgray(), 10, True, 'middle')
    tabs = [('数据表格',cx+20),('可视化图表',cx+170),('总览对比',cx+320)]
    for i,(tab,x) in enumerate(tabs):
        c += text(x, 175, tab, navy() if i==1 else dgray(), 12)
        if i==1: c += rect(x, 197, 60, 3, fill=navy())
    # 柱状图
    y = 215
    c += f'<rect x="{cx+20}" y="{y}" width="540" height="360" rx="8" fill="{white()}" stroke="#E8E8E8"/>\n'
    c += text(cx+35, y+18, '各机构总得分排名', navy(), 12, True)
    bars = [('北京',92.3,teal()),('上海',88.9,blue()),('深圳',85.6,'#722ED1'),('杭州',82.1,orange()),('成都',78.4,gray())]
    bx = cx+35; bh_max = 260
    for nm, sc, col in bars:
        bh = int(250*sc/100)
        c += rect(bx, y+30+250-bh, 60, bh, fill=col)
        c += text(bx+30, y+280-bh-18, f'{sc}', dgray(), 9, True, 'middle')
        c += text(bx+30, y+288, nm, dgray(), 9, True, 'middle')
        bx += 85
    # 雷达图
    c += f'<rect x="{cx+580}" y="{y}" width="540" height="360" rx="8" fill="{white()}" stroke="#E8E8E8"/>\n'
    c += text(cx+595, y+18, '维度得分对比（雷达图）', navy(), 12, True)
    rcx, rcy, rr = cx+850, y+180, 110
    for angle in range(0,360,60):
        a = math.radians(angle)
        c += line(rcx, rcy, rcx+int(rr*math.cos(a)), rcy+int(rr*math.sin(a)), gray(), 1)
    c += f'<circle cx="{rcx}" cy="{rcy}" r="{rr}" fill="none" stroke="{gray()}" stroke-width="1"/>\n'
    pts_bj = [(rcx+int(rr*v*math.cos(math.radians(i*90))),rcy+int(rr*v*math.sin(math.radians(i*90)))) for i,v in enumerate([0.92,0.88,0.85,0.90])]
    pts_str = ' '.join([f'{x},{y}' for x,y in pts_bj])
    c += f'<polygon points="{pts_str}" fill="rgba(4,138,129,0.3)" stroke="{teal()}" stroke-width="2"/>\n'
    pts_sh = [(rcx+int(rr*v*math.cos(math.radians(i*90))),rcy+int(rr*v*math.sin(math.radians(i*90)))) for i,v in enumerate([0.88,0.85,0.82,0.88])]
    pts_str2 = ' '.join([f'{x},{y}' for x,y in pts_sh])
    c += f'<polygon points="{pts_str2}" fill="rgba(24,144,255,0.3)" stroke="{blue()}" stroke-width="2"/>\n'
    c += text(cx+595, y+340, '■ 北京分行', teal(), 10)
    c += text(cx+700, y+340, '■ 上海分行', blue(), 10)
    c += text(cx+805, y+340, '■ 深圳分行', '#722ED1', 10)
    # 进度条
    y2 = y+375
    c += f'<rect x="{cx+20}" y="{y2}" width="1100" height="165" rx="8" fill="{white()}" stroke="#E8E8E8"/>\n'
    c += text(cx+35, y2+18, '关键指标完成率对比', navy(), 12, True)
    for i,(nm,pct,col) in enumerate([('日均存款余额',0.942,teal()),('存款增长率',0.880,blue()),
                                      ('不良贷款率',0.960,green()),('拨备覆盖率',0.920,'#722ED1')]):
        iy = y2+45+i*30
        c += text(cx+35, iy, nm, dgray(), 10)
        pw = int(600*pct)
        c += rect(cx+180, iy+2, 600, 16, fill=lgray(), stroke='#E8E8E8')
        c += rect(cx+180, iy+2, pw, 16, fill=col)
        c += text(cx+790, iy, f'{int(pct*100)}%', dgray(), 10)
    c += footer()
    with open(f'{OUT_DIR}/07_report.svg', 'w') as f: f.write(c)
    print('07_report.svg done')

# ============ Page 8: 通知中心 ============
def page_notifications():
    c = svg_header(1200, 800)
    c += rect(0, 0, 1200, 800, fill='#F8F9FA')
    c += rect(0, 0, 1200, 60, fill=navy())
    c += text(30, 32, '机构绩效管理系统', white(), 16, True)
    c += rect(660, 22, 22, 20, fill='#F5222D')
    c += text(671, 36, '3', white(), 10, True)
    c += text(700, 32, '管理员：张三  |  退出', '#B4CCE8', 11)
    c += rect(0, 60, 200, 740, fill=white(), stroke='#E8E8E8')
    for i, t in enumerate(['首页仪表盘', '考核体系管理', '月度考核监测', '绩效结果展示', '通知中心']):
        y = 80+i*50; col = teal() if i==4 else dgray()
        if i==4: c += rect(0, y, 200, 44, fill=col)
        c += text(20, y+26, t, col if i!=4 else white(), 12)
    cx = 220
    c += text(cx+560, 88, '通知中心', navy(), 20, True, 'middle')
    c += text(cx+20, 120, '共 15 条通知，3 条未读', dgray(), 12)
    for i,(tab,x) in enumerate([('全部',cx+20),('未读',cx+100)]):
        c += text(x, 152, tab, navy() if i==1 else dgray(), 12)
        if i==1: c += rect(x, 174, 30, 3, fill=navy())
        c += f'<rect x="{x+35}" y="{150}" width="18" height="18" rx="9" fill="#F5222D"/>\n'
        c += text(x+44, 163, '3', white(), 9, True)
    c += text(cx+870, 152, '全部标为已读', blue(), 11)
    notifs = [
        ('📊','【收数任务】您有新的数据待填写','体系：2026年度分行绩效考核体系，月份：2026年3月，请于03月27日18:00前完成。','3分钟前',True,'task'),
        ('🔔','【截止提醒】收数即将截止','您的北京分行存款数据还未提交，距离截止还有30分钟，请尽快填写。','30分钟前',True,'reminder'),
        ('✅','【确认通知】请确认本机构绩效数据','2026年2月绩效数据已生成，请确认北京分行的绩效数据。','2小时前',False,'confirm'),
        ('📢','【发布通知】2026年2月绩效结果已发布','管理员已发布2026年2月的绩效结果，数据现已公开，请查看。','1天前',False,'publish'),
    ]
    for i,(icon,title,content,time,unread,typ) in enumerate(notifs):
        y = 185+i*120
        c += f'<rect x="{cx+20}" y="{y}" width="1100" height="105" rx="8" fill="{white()}" stroke="#E8E8E8"/>\n'
        if unread: c += rect(cx+20, y, 5, 105, fill=blue())
        c += text(cx+40, y+22, icon, dgray(), 22)
        c += text(cx+75, y+20, title, navy(), 12, True)
        c += text(cx+75, y+46, content, dgray(), 10)
        c += text(cx+75, y+82, time, '#AAAAAA', 10)
        if unread: c += text(cx+920, y+82, '标为已读', blue(), 10)
        c += text(cx+1020, y+82, '查看详情 →', blue(), 10)
    yy = 185+4*120+20
    c += text(cx+20, yy, '共 15 条', dgray(), 10)
    for i in range(2):
        x = 500+i*40; bg = navy() if i==0 else white(); fc = white() if i==0 else dgray()
        c += f'<rect x="{x}" y="{yy-5}" width="30" height="24" rx="4" fill="{bg}" stroke="#E0E0E0"/>\n'
        c += text(x+15, yy+10, str(i+1), fc, 10, True, 'middle')
    c += text(570, yy, '下一页', dgray(), 10)
    c += footer()
    with open(f'{OUT_DIR}/08_notifications.svg', 'w') as f: f.write(c)
    print('08_notifications.svg done')

# Run
page_login()
page_dashboard()
page_system_list()
page_monitoring_create()
page_monitoring_detail()
page_collect()
page_report()
page_notifications()
print('\nAll SVG mockups generated in', OUT_DIR)
