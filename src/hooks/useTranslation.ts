import { useCallback } from "react";
import { TRANSLATIONS, TranslationKey } from "../constants/translations";

/**
 * Hook for accessing translations
 * In production, this would be connected to an i18n library like react-i18next
 *
 * @returns Object with translation functions
 */
export const useTranslation = () => {
  /**
   * Get translated string for a given key
   * @param key - Translation key
   * @param params - Optional parameters for string interpolation
   * @returns Translated string
   */
  const t = useCallback(
    (key: TranslationKey, params?: Record<string, string | number>): string => {
      const translation = TRANSLATIONS[key];

      if (!translation) {
        console.warn(`Translation key "${key}" not found`);
        return key;
      }

      // Simple parameter replacement
      // In production, use a proper i18n library for complex interpolation
      if (params) {
        return Object.entries(params).reduce((str, [paramKey, value]) => {
          return str.replace(new RegExp(`{{${paramKey}}}`, "g"), String(value));
        }, translation);
      }

      return translation;
    },
    [],
  );

  /**
   * Get translated string with pluralization
   * @param key - Base translation key
   * @param count - Count for pluralization
   * @param params - Optional parameters
   * @returns Translated string with pluralization
   */
  const tPlural = useCallback(
    (
      key: TranslationKey,
      count: number,
      params?: Record<string, string | number>,
    ): string => {
      // For now, simple implementation
      // In production, use proper pluralization rules
      const translation = t(key, { ...params, count });
      return count === 1 ? translation : translation + "s"; // Very basic pluralization
    },
    [t],
  );

  /**
   * Get translated string for time formatting
   * @param seconds - Time in seconds
   * @returns Formatted time string
   */
  const tTime = useCallback(
    (seconds: number): string => {
      const hours = Math.floor(seconds / 3600);
      const minutes = Math.floor((seconds % 3600) / 60);
      const secs = seconds % 60;

      const parts: string[] = [];

      if (hours > 0) {
        parts.push(`${hours}${t("hours")}`);
      }
      if (minutes > 0) {
        parts.push(`${minutes}${t("minutes")}`);
      }
      if (secs > 0 || parts.length === 0) {
        parts.push(`${secs}${t("seconds")}`);
      }

      return parts.join(" ");
    },
    [t],
  );

  return {
    t,
    tPlural,
    tTime,
  };
};
