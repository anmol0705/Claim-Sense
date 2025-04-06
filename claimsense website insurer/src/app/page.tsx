'use client';

import React, { useState, useEffect } from 'react';
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { EyeIcon, EyeOffIcon } from 'lucide-react';
import Link from 'next/link';
import { signInWithGoogle, signUpWithEmailPassword } from "../lib/auth";
import { useRouter } from 'next/navigation';

const SignUpPage = () => {
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    driverLicense: '',
    password: '',
    confirmPassword: '',
    acceptTerms: false
  });

  useEffect(() => {
    // Check if user is already logged in
    const storedUser = localStorage.getItem("user");
    if (storedUser) {
      // If user is already logged in, redirect to dashboard
      router.push('/Dashboard');
    }
  }, [router]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    setError(null); // Clear error when user makes changes
  };

  const handleCheckboxChange = (checked: boolean) => {
    setFormData(prev => ({ ...prev, acceptTerms: checked }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    
    // Basic validation
    if (!formData.email || !formData.password) {
      setError("Please fill in all required fields");
      return;
    }
    
    if (formData.password !== formData.confirmPassword) {
      setError("Passwords do not match");
      return;
    }
    
    if (!formData.acceptTerms) {
      setError("You must accept the terms and privacy policy");
      return;
    }
    
    try {
      setLoading(true);
      const newUser = await signUpWithEmailPassword(formData.email, formData.password, {
        username: formData.username,
        driverLicense: formData.driverLicense
      });
      
      if (newUser) {
        localStorage.setItem("user", JSON.stringify(newUser));
        // Redirect to Dashboard after successful signup
        router.push('/Dashboard');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred during signup");
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    try {
      setLoading(true);
      setError(null);
      const googleUser = await signInWithGoogle();
      if (googleUser) {
        localStorage.setItem("user", JSON.stringify(googleUser));
        // Redirect to Dashboard after successful Google sign-in
        router.push('/Dashboard');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred with Google sign-in");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-black text-white flex flex-col p-6">
      {/* Form */}
      <div className="flex-1">
        <h1 className="text-4xl font-bold mb-8">Create Account</h1>
        
        {error && (
          <div className="mb-4 p-4 bg-red-900/50 border border-red-700 rounded-lg text-red-200">
            {error}
          </div>
        )}
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Input
              placeholder="Username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              className="p-6 rounded-lg text-lg bg-gray-900 border-gray-700 text-white"
              disabled={loading}
            />
          </div>
          
          <div>
            <Input
              placeholder="Email"
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              className="p-6 rounded-lg text-lg bg-gray-900 border-gray-700 text-white"
              required
              disabled={loading}
            />
          </div>
          
          <div>
            <Input
              placeholder="Driver License"
              name="driverLicense"
              value={formData.driverLicense}
              onChange={handleChange}
              className="p-6 rounded-lg text-lg bg-gray-900 border-gray-700 text-white"
              disabled={loading}
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
              required
              disabled={loading}
            />
            <button 
              type="button"
              className="absolute right-4 top-1/2 transform -translate-y-1/2 text-gray-400"
              onClick={() => setShowPassword(!showPassword)}
              disabled={loading}
            >
              {showPassword ? <EyeOffIcon size={20} /> : <EyeIcon size={20} />}
            </button>
          </div>
          
          <div>
            <Input
              placeholder="Confirm Password"
              type="password"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              className="p-6 rounded-lg text-lg bg-gray-900 border-gray-700 text-white"
              required
              disabled={loading}
            />
          </div>
          
          <div className="flex items-center space-x-2 pt-2">
            <Checkbox 
              id="terms" 
              checked={formData.acceptTerms}
              onCheckedChange={handleCheckboxChange}
              className="h-5 w-5 border-2 border-gray-600 data-[state=checked]:bg-blue-600"
              disabled={loading}
            />
            <label htmlFor="terms" className="text-gray-400">
              I accept the <Link href="/terms" className="text-blue-400 hover:text-blue-300">terms and privacy policy</Link>
            </label>
          </div>
          
          <Button 
            type="submit" 
            className="w-full p-6 text-lg bg-blue-600 text-white hover:bg-blue-700 rounded-lg mt-4"
            disabled={loading}
          >
            {loading ? "Processing..." : "Sign Up"}
          </Button>
          
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-700"></div>
            </div>
            <div className="relative flex justify-center">
              <span className="bg-black px-4 text-gray-400">or continue with</span>
            </div>
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
        </form>
        
        <div className="text-center mt-8">
          <p className="text-gray-400">
            Already have an account? <Link href="/signin" className="text-blue-400 hover:text-blue-300 font-medium">Log in</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default SignUpPage;