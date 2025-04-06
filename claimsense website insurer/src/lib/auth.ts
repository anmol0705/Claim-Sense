// lib/auth.ts
import { initializeApp } from "firebase/app";
import { 
  getAuth, 
  GoogleAuthProvider, 
  signInWithPopup, 
  createUserWithEmailAndPassword, 
  signOut,
  updateProfile
} from "firebase/auth";
import { getFirestore, doc, setDoc } from "firebase/firestore";

// Your Firebase configuration
const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const firestore = getFirestore(app);

// Sign in with Google
export const signInWithGoogle = async () => {
  const provider = new GoogleAuthProvider();
  try {
    const result = await signInWithPopup(auth, provider);
    const user = result.user;
    
    // Store additional user info in Firestore
    await setDoc(doc(firestore, "users", user.uid), {
      uid: user.uid,
      displayName: user.displayName,
      email: user.email,
      photoURL: user.photoURL,
      authProvider: "google",
      timestamp: new Date()
    }, { merge: true });
    
    return user;
  } catch (error) {
    console.error("Error signing in with Google:", error);
    throw error;
  }
};

// Sign up with email/password
export const signUpWithEmailPassword = async (
  email: string, 
  password: string, 
  additionalData?: { 
    username?: string;
    driverLicense?: string;
  }
) => {
  try {
    const result = await createUserWithEmailAndPassword(auth, email, password);
    const user = result.user;
    
    // Update profile if username is provided
    if (additionalData?.username) {
      await updateProfile(user, {
        displayName: additionalData.username
      });
    }
    
    // Store user data in Firestore
    await setDoc(doc(firestore, "users", user.uid), {
      uid: user.uid,
      displayName: additionalData?.username || null,
      email: user.email,
      driverLicense: additionalData?.driverLicense || null,
      authProvider: "email",
      timestamp: new Date()
    });
    
    return user;
  } catch (error) {
    console.error("Error signing up with email/password:", error);
    throw error;
  }
};

// Sign out
export const logout = async () => {
  try {
    await signOut(auth);
  } catch (error) {
    console.error("Error signing out:", error);
    throw error;
  }
};

// Check if user is logged in
export const getCurrentUser = () => {
  return auth.currentUser;
};