import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';

/* =========================
   고정 설정 (필요시 여기만 수정)
   ========================= */
const CONFIG = {
    baseUrl: 'http://host.docker.internal:8080',
    targetRps: 40,
    signupRatio: 0.2,           // 전체 중 회원가입 비율
    testDuration: '1m',
    pregenUsers: 100,            // 로그인용 사전 생성 계정 수
    insecureTLS: false,

    expectSignupCode: 'USER_201',
    expectLoginCode:  '',

    // 로그인 성공 후 체인 호출
    doChain: true,
    chain: { method: 'GET', path: '/api/user/profile', body: '', repeats: 3, useDiag: false, diagMs: 50 },

    token: {
        scheme: 'Bearer',
        // 우선 헤더에서 찾고, 없으면 아래 json 필드 후보들에서 순서대로 탐색
        jsonFieldCandidates: ['accessToken', 'data.accessToken', 'token', 'jwt', 'result.accessToken'],
    },

    // 디버그/토글
    debug: false,
    disableLogin: false,
    disableSignup: false,

    // setup 배치 크기
    setupBatch: 50,
};

/* =========================
   메트릭
   ========================= */
const signupDuration = new Trend('signup_duration');
const loginDuration  = new Trend('login_duration');
const chainDuration = new Trend('chain_duration');
const chainFailRate = new Rate('chain_fail_rate');
const signupFailRate = new Rate('signup_fail_rate');
const loginFailRate  = new Rate('login_fail_rate');
const signupCount    = new Counter('signup_count');
const loginCount     = new Counter('login_count');

/* =========================
   부하 비율
   ========================= */
const SIGNUP_RPS = Math.max(0, Math.floor(CONFIG.targetRps * CONFIG.signupRatio));
const LOGIN_RPS  = Math.max(0, CONFIG.targetRps - SIGNUP_RPS);

/* =========================
   시나리오 구성
   ========================= */
const scenarios = {};
if (!CONFIG.disableSignup && SIGNUP_RPS > 0) {
    scenarios.signup_scn = {
        executor: 'ramping-arrival-rate',
        startRate: Math.max(1, Math.floor(SIGNUP_RPS * 0.2)),
        timeUnit: '1s',
        preAllocatedVUs: Math.max(10, SIGNUP_RPS * 2),
        maxVUs: Math.max(200, SIGNUP_RPS * 5),
        stages: [
            { duration: '30s', target: SIGNUP_RPS },
            { duration: CONFIG.testDuration, target: SIGNUP_RPS },
            { duration: '30s', target: 0 },
        ],
        gracefulStop: '30s',
        exec: 'signUp',
    };
}
if (!CONFIG.disableLogin && LOGIN_RPS > 0) {
    scenarios.login_scn = {
        executor: 'ramping-arrival-rate',
        startRate: Math.max(1, Math.floor(LOGIN_RPS * 0.2)),
        timeUnit: '1s',
        preAllocatedVUs: Math.max(20, LOGIN_RPS * 2),
        maxVUs: Math.max(200, LOGIN_RPS * 5),
        stages: [
            { duration: '30s', target: LOGIN_RPS },
            { duration: CONFIG.testDuration, target: LOGIN_RPS },
            { duration: '30s', target: 0 },
        ],
        gracefulStop: '30s',
        exec: 'logIn',
    };
}

/* =========================
   k6 options
   ========================= */
export const options = {
    discardResponseBodies: !CONFIG.debug,
    setupTimeout: '10m',
    insecureSkipTLSVerify: CONFIG.insecureTLS,
    scenarios,
    thresholds: {
        http_req_failed: ['rate<0.02'],
        'login_duration{endpoint:login}': ['p(95)<500'],
        'signup_duration{endpoint:signup}': ['p(95)<800'],
        'chain_duration{endpoint:chain}': ['p(95)<500'],
        signup_fail_rate: ['rate<0.05'],
        login_fail_rate: ['rate<0.02'],
        chain_fail_rate: ['rate<0.02'],
    },
};

/* =========================
   Helpers
   ========================= */
function randomLowerAlnum(len) {
    const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
    let s = ''; for (let i = 0; i < len; i++) s += chars[(Math.random() * chars.length) | 0];
    return s;
}
function randomDigits(len) {
    let s = ''; for (let i = 0; i < len; i++) s += (Math.random() * 10) | 0;
    return s;
}
function hashCode(s) {
    let h = 0; for (let i = 0; i < s.length; i++) { h = ((h << 5) - h) + s.charCodeAt(i); h |= 0; }
    return Math.abs(h);
}
function uniqueSuffix() {
    const rnd = Math.random().toString(36).slice(2, 8);
    return `${Date.now()}_${rnd}_${randomDigits(6)}`;
}
function genPassword() {
    // 8~15자, 영문/숫자/특수문자 포함 (12자로 생성)
    const lowers='abcdefghijklmnopqrstuvwxyz', uppers='ABCDEFGHIJKLMNOPQRSTUVWXYZ',
        digits='0123456789', specs='!@#$%^&*';
    const pick = (s) => s[(Math.random() * s.length) | 0];
    const all = lowers + uppers + digits + specs;
    let a = [pick(lowers), pick(uppers), pick(digits), pick(specs)];
    while (a.length < 12) a.push(pick(all));
    for (let i = a.length - 1; i > 0; i--) { const j = (Math.random() * (i + 1)) | 0; [a[i], a[j]] = [a[j], a[i]]; }
    return a.join('');
}
function jsonHeaders(extra = {}) {
    return { headers: { 'Content-Type': 'application/json', 'Accept': 'application/json', ...extra } };
}

function getAccessToken(res) {
    // 1) Authorization 헤더 시도
    const h = res.headers['Authorization'] || res.headers['authorization'];
    if (h && h.includes(' ')) return h.split(' ')[1];
    if (h && !h.includes(' ')) return h; // 스킴 없이 토큰만 내려올 수도 있음

    // 2) 바디 JSON 후보 키 탐색
    try {
        const b = res.json();
        for (const key of CONFIG.token.jsonFieldCandidates) {
            const parts = key.split('.');
            let v = b, ok = true;
            for (const p of parts) {
                if (v && Object.prototype.hasOwnProperty.call(v, p)) v = v[p];
                else { ok = false; break; }
            }
            if (ok && typeof v === 'string' && v.length > 0) return v;
        }
    } catch (_) { /* not json or parse error */ }

    return null;
}

function checkOk(res, expected) {
    // 2xx면 성공. expected가 있으면 코드 불일치도 통과하되 debug에 경고
    const okStatus = res.status >= 200 && res.status < 300;
    if (!okStatus) return false;
    if (!expected) return true;
    if (CONFIG.debug && res.body) {
        try {
            const b = res.json();
            const code = b?.resultCode || b?.code || b?.status;
            if (code !== expected) console.error('[WARN] expected code:', expected, 'but got:', code);
        } catch (_) {}
    }
    return true;
}

/* =========================
   Payload builders (제약 반영)
   ========================= */
function makeSignupPayload() {
    const suf = uniqueSuffix();
    const phoneTail = String(hashCode(suf)).padStart(8, '0').slice(0, 8); // 8자리

    // 서버 제약:
    // nickname ≤ 10자 → 'nk' + 8자, name 8자, loginId [a-z0-9] 12자
    const nickname = `nk${randomLowerAlnum(8)}`; // 10자
    const name     = `u${randomLowerAlnum(7)}`;  // 8자
    const pw       = genPassword();

    return {
        name,
        nickname,
        phoneNumber: `010${phoneTail}`,        // 필요 시 '010-xxxx-xxxx'
        loginId: `t${randomLowerAlnum(11)}`,   // 총 12자
        password: pw,
        confirmPassword: pw,
    };
}

/* =========================
   setup: 로그인용 계정 선행 생성
   ========================= */
export function setup() {
    const users = [];
    if (CONFIG.disableLogin || CONFIG.pregenUsers <= 0) return { users };

    const CHUNK = CONFIG.setupBatch;
    let debugLogged = 0;

    for (let i = 0; i < CONFIG.pregenUsers; i += CHUNK) {
        const reqs = [];
        const payloads = [];
        const batchSize = Math.min(CHUNK, CONFIG.pregenUsers - i);

        for (let j = 0; j < batchSize; j++) {
            const p = makeSignupPayload();
            payloads.push(p);
            reqs.push(['POST', `${CONFIG.baseUrl}/api/auth/sign-up`, JSON.stringify(p), jsonHeaders()]);
        }

        const responses = http.batch(reqs);
        for (let k = 0; k < responses.length; k++) {
            const r = responses[k];
            if (checkOk(r, CONFIG.expectSignupCode)) {
                users.push({ loginId: payloads[k].loginId, password: payloads[k].password });
            } else if (CONFIG.debug && debugLogged < 5) {
                try {
                    console.error('[SIGNUP FAIL in setup]', 'status=', r.status, 'body=', r.body ? String(r.body).slice(0, 800) : '(no body)');
                } catch { console.error('[SIGNUP FAIL in setup]', 'status=', r.status, '(body unreadable)'); }
                debugLogged++;
            }
        }
    }

    if (!CONFIG.disableLogin && users.length === 0) {
        throw new Error('로그인용 사전 계정 생성 실패. baseUrl 또는 서버 검증 정책 확인 요망.');
    }
    return { users };
}

/* =========================
   회원가입 시나리오
   ========================= */
export function signUp() {
    const payload = makeSignupPayload();
    const res = http.post(`${CONFIG.baseUrl}/api/auth/sign-up`, JSON.stringify(payload), { ...jsonHeaders(), tags: { endpoint: 'signup' } });

    const ok = checkOk(res, CONFIG.expectSignupCode);
    signupDuration.add(res.timings.duration, { endpoint: 'signup' });
    signupCount.add(1);
    signupFailRate.add(ok ? 0 : 1);

    if (CONFIG.debug && !ok) console.error('[SIGNUP FAIL]', 'status=', res.status, 'body=', res.body ? String(res.body).slice(0, 800) : '(no body)');
    sleep(0.1);
}

/* =========================
   로그인 시나리오 (+옵션 체인)
   ========================= */
let loginIdx; // VU 컨텍스트에서 지연 초기화
export function logIn(data) {
    if (CONFIG.disableLogin) return;

    if (loginIdx === undefined) {
        const vuId = (typeof __VU !== 'undefined') ? __VU : 0;
        loginIdx = (Math.random() * 1e6) | 0 + vuId * 1000;
    }

    const users = data.users || [];
    if (users.length === 0) { sleep(0.1); return; }

    const u = users[(loginIdx++) % users.length];
    const res = http.post(
        `${CONFIG.baseUrl}/api/auth/login`,
        JSON.stringify({ loginId: u.loginId, password: u.password }),
        { ...jsonHeaders(), tags: { endpoint: 'login' } },
    );

    const ok = checkOk(res, CONFIG.expectLoginCode);
    loginDuration.add(res.timings.duration, { endpoint: 'login' });
    loginCount.add(1);
    loginFailRate.add(ok ? 0 : 1);

    if (CONFIG.debug && !ok) console.error('[LOGIN FAIL]', 'status=', res.status, 'body=', res.body ? String(res.body).slice(0, 800) : '(no body)');

    if (ok && CONFIG.doChain) {
        const token = getAccessToken(res);
        let params = jsonHeaders();
        if (token) params = jsonHeaders({ 'Authorization': `${CONFIG.token.scheme} ${token}` });
        else if (CONFIG.debug) console.error('[CHAIN] No token found. Sending without Authorization.');

        const repeats = CONFIG.chain.repeats || 1;
        for (let i = 0; i < repeats; i++) {
            const url = CONFIG.chain.useDiag
                ? `${CONFIG.baseUrl}/diag/db-poke?ms=${CONFIG.chain.diagMs || 50}`
                : `${CONFIG.baseUrl}${CONFIG.chain.path}`;

            let chainRes;
            if (CONFIG.chain.method === 'GET')       chainRes = http.get(url, params);
            else if (CONFIG.chain.method === 'POST') chainRes = http.post(url, CONFIG.chain.body, params);
            else if (CONFIG.chain.method === 'PUT')  chainRes = http.put(url, CONFIG.chain.body, params);
            else if (CONFIG.chain.method === 'DELETE') chainRes = http.del(url, null, params);

            if (chainRes) {
                chainDuration.add(chainRes.timings.duration, { endpoint: 'chain' });
                chainFailRate.add(chainRes.status >= 200 && chainRes.status < 300 ? 0 : 1);
                if (CONFIG.debug && !(chainRes.status >= 200 && chainRes.status < 300)) {
                    console.error('[CHAIN FAIL]', 'status=', chainRes.status, 'body=', chainRes.body ? String(chainRes.body).slice(0, 400) : '(no body)');
                }
            }
        }
    }
    sleep(0.05);
}
