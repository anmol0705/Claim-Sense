'use client';

import React, { useState, useEffect } from 'react';
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { EyeIcon, EyeOffIcon } from 'lucide-react';
import Link from 'next/link';
import { initializeApp } from "firebase/app";
import { 
  getAuth, 
  signInWithEmailAndPassword, 
  GoogleAuthProvider, 
  signInWithPopup,
  FacebookAuthProvider,
  onAuthStateChanged
} from "firebase/auth";
import { useRouter } from 'next/navigation';

const SignInPage = () => {
  const [showPassword, setShowPassword] = useState(false);
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    rememberMe: false
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  // Firebase configuration - replace with your own config
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
  
  // Providers
  const googleProvider = new GoogleAuthProvider();

  useEffect(() => {
    // Check if user is already signed in
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      if (user) {
        // User is signed in, redirect to dashboard or home
        router.push('/Dashboard');
      }
    });

    return () => unsubscribe();
  }, [auth, router]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    
    try {
      // Sign in with email and password
      const { email, password } = formData;
      await signInWithEmailAndPassword(auth, email, password);
      // Redirect will happen automatically via useEffect
    } catch (error: any) {
      console.error('Error signing in:', error);
      setError(error.message || 'Failed to sign in');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    setLoading(true);
    setError('');
    
    try {
      await signInWithPopup(auth, googleProvider);
      // Redirect will happen automatically via useEffect
    } catch (error: any) {
      console.error('Error signing in with Google:', error);
      setError(error.message || 'Failed to sign in with Google');
    } finally {
      setLoading(false);
    }
  };

  

  
  return (
    <div className="min-h-screen bg-black text-white flex flex-col p-6">
      {/* Form */}
      <div className="flex-1">
        <h1 className="text-4xl font-bold mb-8">Sign In</h1>
        
        {error && (
          <div className="bg-red-900/50 border border-red-500 text-red-100 p-3 rounded-md mb-4">
            {error}
          </div>
        )}
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Input
              placeholder="Email"
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              className="p-6 rounded-lg text-lg bg-gray-900 border-gray-700 text-white"
              disabled={loading}
              required
            />
          </div>
          
          <div className="relative">
            <Input
              placeholder="Password"
              type={showPassword ? "text" : "password"}
              name="password"
              value={formData.password}
              onChange={handleChange}
              className="p-6 rounded-lg text-lg bg-gray-900 border-gray-700 text-white"
              disabled={loading}
              required
            />
            <button 
              type="button"
              className="absolute right-4 top-1/2 transform -translate-y-1/2 text-gray-400"
              onClick={() => setShowPassword(!showPassword)}
            >
              {showPassword ? <EyeOffIcon size={20} /> : <EyeIcon size={20} />}
            </button>
          </div>
          
          <div className="flex justify-between items-center pt-2">
            <div className="flex items-center space-x-2">
              <Checkbox
                id="remember"
                checked={formData.rememberMe}
                onCheckedChange={(checked) => 
                  setFormData(prev => ({ ...prev, rememberMe: checked === true }))
                }
                className="h-4 w-4 border-gray-600 data-[state=checked]:bg-blue-600"
                disabled={loading}
              />
              <label htmlFor="remember" className="text-gray-400">Remember me</label>
            </div>
            <Link href="/forgot-password" className="text-blue-400 hover:text-blue-300">
              Forgot Password?
            </Link>
          </div>
          
          <Button 
            type="submit" 
            className="w-full p-6 text-lg bg-blue-600 text-white hover:bg-blue-700 rounded-lg mt-4"
            disabled={loading}
          >
            {loading ? 'Signing In...' : 'Sign In'}
          </Button>
        </form>
        
        <div className="flex items-center my-8">
          <div className="flex-grow h-px bg-gray-800"></div>
          <div className="px-4 text-gray-400">or continue with</div>
          <div className="flex-grow h-px bg-gray-800"></div>
        </div>
        
        <Button 
                    type="button"
                    onClick={handleGoogleSignIn}
                    className="w-full p-6 text-lg bg-gray-800 text-white hover:bg-gray-700 rounded-lg flex items-center justify-center gap-3"
                    disabled={loading}
                  >
                    <svg width="20" height="20" viewBox="0 0 24 24">
                      <path
                        d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                        fill="#4285F4"
                      />
                      <path
                        d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                        fill="#34A853"
                      />
                      <path
                        d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                        fill="#FBBC05"
                      />
                      <path
                        d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                        fill="#EA4335"
                      />
                    </svg>
                    Sign in with Google
                  </Button>
        
        <div className="text-center mt-8">
          <p className="text-gray-400">
            Don't have an account? <Link href="/" className="text-blue-400 hover:text-blue-300 font-medium">Sign up</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default SignInPage;