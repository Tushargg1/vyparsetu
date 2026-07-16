import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

const resources = {
  en: {
    translation: {
      appName: 'VyaparSetu',
      login: 'Login',
      register: 'Register',
      phone: 'Mobile number',
      quickOrder: 'Quick Order',
      search: 'Search products',
      cart: 'Cart',
      repeat: 'Repeat order',
      voice: 'Voice',
      text: 'Text',
      photo: 'Photo',
      scan: 'Scan',
      inventory: 'Inventory',
      orders: 'Orders',
      home: 'Home',
      aiRecommended: 'AI Recommended',
      logout: 'Logout',
      lowStock: 'Low stock',
    },
  },
  hi: {
    translation: {
      appName: 'व्यापारसेतु',
      login: 'लॉगिन',
      register: 'रजिस्टर',
      phone: 'मोबाइल नंबर',
      quickOrder: 'तुरंत ऑर्डर',
      search: 'प्रोडक्ट खोजें',
      cart: 'कार्ट',
      repeat: 'दोबारा ऑर्डर',
      voice: 'आवाज़',
      text: 'टेक्स्ट',
      photo: 'फोटो',
      scan: 'स्कैन',
      inventory: 'इन्वेंटरी',
      orders: 'ऑर्डर',
      home: 'होम',
      aiRecommended: 'AI सुझाव',
      logout: 'लॉगआउट',
      lowStock: 'कम स्टॉक',
    },
  },
};

i18n.use(initReactI18next).init({
  resources,
  lng: localStorage.getItem('lang') || 'en',
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
});

export default i18n;
