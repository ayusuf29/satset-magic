import { Language, Translations } from "../types";
import { en } from "./en";
import { id } from "./id";

export const translations: Record<Language, Translations> = {
  en,
  id,
};

export { en, id };
