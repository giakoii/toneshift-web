export const APP_NAME = "ToneShift";
export const APP_TAGLINE = "Cảm Âm Pro";
export const APP_DESCRIPTION =
  "Công cụ chuyển đổi cảm âm nhạc Việt Nam hàng đầu. Hỗ trợ sáo trúc, harmonica, guitar và nhiều nhạc cụ khác.";
export const APP_URL = "https://toneshift.vn";

export const ROUTES = {
  HOME: "/",
  CONVERTER: "/converter",
  LIBRARY: "/library",
  COMMUNITY: "/community",
  DOCS: "/docs",
  TOOLS: {
    ROOT: "/tools",
    DETECT: "/tools/detect",
    COMPARE: "/tools/compare",
    PRACTICE: "/tools/practice",
  },
  AUTH: {
    LOGIN: "/login",
    REGISTER: "/register",
    PROFILE: "/profile",
  },
  LEGAL: {
    PRIVACY: "/privacy",
    TERMS: "/terms",
  },
} as const;

export const LOCAL_STORAGE_KEYS = {
  HISTORY: "toneshift_history",
  PREFERRED_TONE: "toneshift_preferred_tone",
  THEME: "toneshift_theme",
} as const;

export const HISTORY_MAX_ITEMS = 50;
