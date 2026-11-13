import {TRANSLATIONS} from './translations.js?v=4.0.0';
export const LOCALES=Object.freeze(['es','ca','en']);
let current='es';
export function resolveLocale(preference='auto',languages=[]){if(LOCALES.includes(preference))return preference;for(const value of languages){const code=String(value).toLowerCase().split(/[-_]/)[0];if(LOCALES.includes(code))return code;}return'en';}
export function setLocale(code){current=LOCALES.includes(code)?code:'en';return current;}
export const getLocale=()=>current;
export const speechLocale=code=>({es:'es-ES',ca:'ca-ES',en:'en-US'}[code]||'en-US');
export function t(source,...values){let key=source;if(Array.isArray(source)&&Object.hasOwn(source,'raw'))key=source.map((part,i)=>part+(i<values.length?`{${i}}`:'')).join('');const text=current==='es'?key:TRANSLATIONS[key]?.[current==='ca'?0:1]??key;return String(text).replace(/\{(\d+)\}/g,(whole,n)=>Number(n)<values.length?String(values[n]):whole);}
export function translateDocument(doc){doc.documentElement.lang=current;for(const el of doc.querySelectorAll('[data-i18n]')){const source=el.getAttribute('data-i18n');const original=el.textContent;el.textContent=(original.match(/^\s*/)?.[0]||'')+t(source)+(original.match(/\s*$/)?.[0]||'');}for(const attr of['aria-label','title','placeholder','content'])for(const el of doc.querySelectorAll(`[data-i18n-${attr}]`))el.setAttribute(attr,t(el.getAttribute('data-i18n-'+attr)));const manifest=doc.querySelector('link[rel="manifest"]');if(manifest)manifest.href=`/manifest-${current}.webmanifest`;doc.documentElement.dataset.localized='true';}
export function friendlyError(error){const message=String(error?.message||'');if(Object.hasOwn(TRANSLATIONS,message)||Object.values(TRANSLATIONS).some(pair=>pair.includes(message)))return t(message);return t('No se pudo completar la operación. Revisa los permisos y vuelve a intentarlo.');}
