// lib/firebaseConfig.js
import { initializeApp, getApps, getApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";

const firebaseConfig = {
    apiKey: "AIzaSyAmsJSvvmedwOFWZM9n3T2T98HoxXpMA5g",
    authDomain: "claim-sense.firebaseapp.com",
    projectId: "claim-sense",
    storageBucket: "claim-sense.firebasestorage.app",
    messagingSenderId: "4793786497",
    appId: "1:4793786497:web:5b6872846196568236ad80",
    measurementId: "G-BSVSJT49Z2",
};

const app = !getApps().length ? initializeApp(firebaseConfig) : getApp();
const auth = getAuth(app);
const db = getFirestore(app);
const storage = getStorage(app);

export { auth, db, storage };
