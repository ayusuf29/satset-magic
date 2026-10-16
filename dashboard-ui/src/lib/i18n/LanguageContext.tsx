"use client";

import React, { createContext, useContext, useState, useEffect, useMemo, ReactNode } from "react";
import { Language, Translations } from "./types";
import { translations, en } from "./translations";

interface LanguageContextType {
  language: Language;
  setLanguage: (lang: Language) => void;
  toggleLanguage: () => void;
  t: Translations;
  isEnglish: boolean;
  isIndonesian: boolean;
}

const STORAGE_KEY = "satset_orchestrator_lang";
const DEFAULT_LANGUAGE: Language = "en"; // Default strictly English as required

const LanguageContext = createContext<LanguageContextType>({
  language: DEFAULT_LANGUAGE,
  setLanguage: () => {},
  toggleLanguage: () => {},
  t: en,
  isEnglish: true,
  isIndonesian: false,
});

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>(DEFAULT_LANGUAGE);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored === "en" || stored === "id") {
        setLanguageState(stored);
        document.documentElement.lang = stored;
      } else {
        document.documentElement.lang = DEFAULT_LANGUAGE;
      }
    } catch {
      // Ignore localStorage errors (e.g. private mode or SSR)
    }
    setMounted(true);
  }, []);

  const setLanguage = (lang: Language) => {
    setLanguageState(lang);
    try {
      localStorage.setItem(STORAGE_KEY, lang);
      if (typeof document !== "undefined") {
        document.documentElement.lang = lang;
      }
    } catch (err) {
      console.warn("Failed to persist language preference:", err);
    }
  };

  const toggleLanguage = () => {
    const nextLang: Language = language === "en" ? "id" : "en";
    setLanguage(nextLang);
  };

  const currentTranslations = useMemo(() => {
    return translations[language] || en;
  }, [language]);

  const value = useMemo(
    () => ({
      language,
      setLanguage,
      toggleLanguage,
      t: currentTranslations,
      isEnglish: language === "en",
      isIndonesian: language === "id",
    }),
    [language, currentTranslations]
  );

  return (
    <LanguageContext.Provider value={value}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage(): LanguageContextType {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error("useLanguage must be used within a LanguageProvider");
  }
  return context;
}

export * from "./types";
