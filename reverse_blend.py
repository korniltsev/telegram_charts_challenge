from z3 import *

from z3 import *
import IPython
s = Solver()

ff = Real('ff')
one = Real('one')
s.add(ff == 255)
s.add(one == 1)

bg1 = [0xff, 0xff, 0xff]
res1 = [0xF5, 0xF8, 0xF9]

bg2 = [0xED, 0x68, 0x5F]
res2 = [0xF1, 0xD2, 0xD1]

alpha = Real('x_alpha')
color = [Real('x_r'), Real('x_g'), Real('x_b')]


def add_color(c, name):
    for i in range(3):
        it_name = name + "_" +("rgb"[i])
        v = Real(it_name)
        f = c[i] / 255.0
        s.add(v >= f-0.0009)
        s.add(v <= f+0.0009)
        s.add(v >=0 )
        s.add(v <=1)

def add_blend(res_name, bg_name):
    for i in range(3):
        it_res_name = res_name + "_" +("rgb"[i])
        it_bg_name = bg_name + "_" +("rgb"[i])
        c = color[i]
        s.add(c <= 1.0)
        s.add(Real(it_res_name) == c * alpha + (one - alpha) * Real(it_bg_name))

add_color(res1, "res1")
add_color(bg1, "bg1")
add_blend("res1", "bg1")

add_color(res2, "res2")
add_color(bg2, "bg2")
add_blend("res2", "bg2")

s.add(alpha < 1)
s.add(alpha > 0)


print s.check()
print s.model()
real_alpha = float(s.model().eval(alpha).as_decimal(8))
r = float(s.model().eval(color[0]).as_decimal(18)[:17])
g = float(s.model().eval(color[1]).as_decimal(18)[:17])
b = float(s.model().eval(color[2]).as_decimal(18)[:17])

print real_alpha * 100
print  hex(int(255 * r))[2:]+hex(int(255* g))[2:]+ hex(int(255 * b))[2:]
