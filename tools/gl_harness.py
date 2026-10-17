import ctypes as C, numpy as np, math, sys
from PIL import Image

EGL = C.CDLL("libEGL.so.1")
EGL.eglGetProcAddress.restype = C.c_void_p
EGL.eglGetProcAddress.argtypes = [C.c_char_p]
def egl_fn(name, restype, argtypes):
    p = EGL.eglGetProcAddress(name.encode())
    if not p: raise RuntimeError("sem " + name)
    return C.CFUNCTYPE(restype, *argtypes)(p)

getPlat = egl_fn("eglGetPlatformDisplayEXT", C.c_void_p, [C.c_uint, C.c_void_p, C.c_void_p])
dpy = getPlat(0x31DD, None, None)   # EGL_PLATFORM_SURFACELESS_MESA
EGL.eglInitialize.argtypes = [C.c_void_p, C.POINTER(C.c_int), C.POINTER(C.c_int)]
maj, mnr = C.c_int(), C.c_int()
assert EGL.eglInitialize(C.c_void_p(dpy), C.byref(maj), C.byref(mnr)), "eglInitialize falhou"
EGL.eglBindAPI(0x30A2)
cfg_attr = (C.c_int * 7)(0x3033, 0x1, 0x3040, 0x8, 0x3038, 0, 0)  # pbuffer + opengl
cfg = C.c_void_p(); n = C.c_int()
EGL.eglChooseConfig.argtypes = [C.c_void_p, C.POINTER(C.c_int), C.POINTER(C.c_void_p), C.c_int, C.POINTER(C.c_int)]
assert EGL.eglChooseConfig(C.c_void_p(dpy), cfg_attr, C.byref(cfg), 1, C.byref(n)) and n.value > 0, "sem config"
ctx_attr = (C.c_int * 7)(0x3098, 3, 0x30FB, 3, 0x30FD, 1, 0x3038)
EGL.eglCreateContext.restype = C.c_void_p
EGL.eglCreateContext.argtypes = [C.c_void_p, C.c_void_p, C.c_void_p, C.POINTER(C.c_int)]
ctx = EGL.eglCreateContext(C.c_void_p(dpy), cfg, None, ctx_attr)
assert ctx, "contexto falhou"
pb_attr = (C.c_int * 5)(0x3057, 16, 0x3056, 16, 0x3038)
EGL.eglCreatePbufferSurface.restype = C.c_void_p
EGL.eglCreatePbufferSurface.argtypes = [C.c_void_p, C.c_void_p, C.POINTER(C.c_int)]
surf = EGL.eglCreatePbufferSurface(C.c_void_p(dpy), cfg, pb_attr)
EGL.eglMakeCurrent.argtypes = [C.c_void_p, C.c_void_p, C.c_void_p, C.c_void_p]
assert EGL.eglMakeCurrent(C.c_void_p(dpy), C.c_void_p(surf), C.c_void_p(surf), C.c_void_p(ctx)), "makeCurrent falhou"

def glf(name, restype, argtypes): return egl_fn(name, restype, argtypes)
glGetString = glf("glGetString", C.c_char_p, [C.c_uint])
print("GL:", glGetString(0x1F01).decode(), "|", glGetString(0x1F02).decode(), "| GLSL", glGetString(0x8B8C).decode())

f = {}
def reg(name, res, args): f[name] = glf(name, res, args)
U, I, F, P, S = C.c_uint, C.c_int, C.c_float, C.c_void_p, C.c_char_p
reg("glCreateShader", U, [U]); reg("glShaderSource", None, [U, I, C.POINTER(S), C.POINTER(I)])
reg("glCompileShader", None, [U]); reg("glGetShaderiv", None, [U, U, C.POINTER(I)])
reg("glGetShaderInfoLog", None, [U, I, C.POINTER(I), S]); reg("glCreateProgram", U, [])
reg("glAttachShader", None, [U, U]); reg("glLinkProgram", None, [U]); reg("glGetProgramiv", None, [U, U, C.POINTER(I)])
reg("glGetProgramInfoLog", None, [U, I, C.POINTER(I), S]); reg("glUseProgram", None, [U])
reg("glGetUniformLocation", I, [U, S]); reg("glUniform4f", None, [I, F, F, F, F]); reg("glUniform1i", None, [I, I])
reg("glUniformMatrix4fv", None, [I, I, C.c_ubyte, C.POINTER(F)])
reg("glGenVertexArrays", None, [I, C.POINTER(U)]); reg("glBindVertexArray", None, [U])
reg("glGenBuffers", None, [I, C.POINTER(U)]); reg("glBindBuffer", None, [U, U])
reg("glBufferData", None, [U, C.c_ssize_t, P, U]); reg("glEnableVertexAttribArray", None, [U])
reg("glVertexAttribPointer", None, [U, I, U, C.c_ubyte, I, P]); reg("glBindAttribLocation", None, [U, U, S])
reg("glGenTextures", None, [I, C.POINTER(U)]); reg("glBindTexture", None, [U, U]); reg("glActiveTexture", None, [U])
reg("glTexImage2D", None, [U, I, I, I, I, I, U, U, P]); reg("glTexParameteri", None, [U, U, I])
reg("glGenFramebuffers", None, [I, C.POINTER(U)]); reg("glBindFramebuffer", None, [U, U])
reg("glFramebufferTexture2D", None, [U, U, U, U, I]); reg("glCheckFramebufferStatus", U, [U])
reg("glViewport", None, [I, I, I, I]); reg("glClearColor", None, [F, F, F, F]); reg("glClear", None, [U])
reg("glEnable", None, [U]); reg("glDisable", None, [U]); reg("glBlendFuncSeparate", None, [U, U, U, U])
reg("glDrawArrays", None, [U, I, I]); reg("glReadPixels", None, [I, I, I, I, U, U, P]); reg("glFinish", None, [])
reg("glGetError", U, [])

def compile_shader(kind, src):
    sh = f["glCreateShader"](kind)
    b = C.c_char_p(src.encode()); ln = C.c_int(len(src))
    f["glShaderSource"](sh, 1, C.byref(b), C.byref(ln)); f["glCompileShader"](sh)
    ok = C.c_int(); f["glGetShaderiv"](sh, 0x8B81, C.byref(ok))
    buf = C.create_string_buffer(8192); f["glGetShaderInfoLog"](sh, 8192, None, buf)
    log = buf.value.decode(errors="replace")
    return sh, ok.value, log

def build(vsrc, fsrc):
    vs, ok1, l1 = compile_shader(0x8B31, vsrc)
    fs, ok2, l2 = compile_shader(0x8B30, fsrc)
    print("vertex  :", "OK" if ok1 else "FALHOU", l1.strip())
    print("fragment:", "OK" if ok2 else "FALHOU", l2.strip())
    if not (ok1 and ok2): return None
    pr = f["glCreateProgram"](); f["glAttachShader"](pr, vs); f["glAttachShader"](pr, fs)
    f["glBindAttribLocation"](pr, 0, b"Position"); f["glLinkProgram"](pr)
    ok = C.c_int(); f["glGetProgramiv"](pr, 0x8B82, C.byref(ok))
    buf = C.create_string_buffer(8192); f["glGetProgramInfoLog"](pr, 8192, None, buf)
    print("link    :", "OK" if ok.value else "FALHOU", buf.value.decode(errors="replace").strip())
    return pr if ok.value else None

def render(pr, W, H, uniforms, depth_img, bg_rgba=(0, 0, 0, 0)):
    vao = U(); f["glGenVertexArrays"](1, C.byref(vao)); f["glBindVertexArray"](vao)
    quad = np.array([-1,-1,0, 1,-1,0, 1,1,0, -1,-1,0, 1,1,0, -1,1,0], dtype=np.float32)
    vbo = U(); f["glGenBuffers"](1, C.byref(vbo)); f["glBindBuffer"](0x8892, vbo)
    f["glBufferData"](0x8892, quad.nbytes, quad.ctypes.data_as(P), 0x88E4)
    f["glEnableVertexAttribArray"](0); f["glVertexAttribPointer"](0, 3, 0x1406, 0, 0, None)
    # textura de profundidade (terreno)
    dt = U(); f["glGenTextures"](1, C.byref(dt)); f["glActiveTexture"](0x84C0); f["glBindTexture"](0xDE1, dt)
    d = np.ascontiguousarray(np.flipud(depth_img).astype(np.float32))
    f["glTexImage2D"](0xDE1, 0, 0x8CAC, W, H, 0, 0x1902, 0x1406, d.ctypes.data_as(P))
    for pn in (0x2800, 0x2801): f["glTexParameteri"](0xDE1, pn, 0x2600)
    for pn in (0x2802, 0x2803): f["glTexParameteri"](0xDE1, pn, 0x812F)
    # alvo de cor
    ct = U(); f["glGenTextures"](1, C.byref(ct)); f["glBindTexture"](0xDE1, ct)
    f["glTexImage2D"](0xDE1, 0, 0x8058, W, H, 0, 0x1908, 0x1401, None)
    for pn in (0x2800, 0x2801): f["glTexParameteri"](0xDE1, pn, 0x2600)
    fb = U(); f["glGenFramebuffers"](1, C.byref(fb)); f["glBindFramebuffer"](0x8D40, fb)
    f["glFramebufferTexture2D"](0x8D40, 0x8CE0, 0xDE1, ct, 0)
    st = f["glCheckFramebufferStatus"](0x8D40); assert st == 0x8CD5, hex(st)
    f["glViewport"](0, 0, W, H); f["glClearColor"](*bg_rgba); f["glClear"](0x4000)
    f["glEnable"](0xBE2); f["glBlendFuncSeparate"](1, 0x0303, 1, 0x0303); f["glDisable"](0xB71)
    f["glUseProgram"](pr)
    f["glActiveTexture"](0x84C0); f["glBindTexture"](0xDE1, dt)
    f["glUniform1i"](f["glGetUniformLocation"](pr, b"DepthSampler"), 0)
    for name, val in uniforms.items():
        loc = f["glGetUniformLocation"](pr, name.encode())
        if loc < 0: print("  (uniform inativo:", name, ")"); continue
        if name == "CamRot":
            m = np.ascontiguousarray(np.array(val, dtype=np.float32).T)  # column-major
            f["glUniformMatrix4fv"](loc, 1, 0, m.ctypes.data_as(C.POINTER(F)))
        else: f["glUniform4f"](loc, *[float(x) for x in val])
    f["glDrawArrays"](0x0004, 0, 6); f["glFinish"]()
    err = f["glGetError"](); print("glError:", hex(err))
    buf = np.zeros((H, W, 4), dtype=np.uint8)
    f["glReadPixels"](0, 0, W, H, 0x1908, 0x1401, buf.ctypes.data_as(P))
    return np.flipud(buf)
