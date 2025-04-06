// app/dashboard/page.tsx
'use client';

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { format } from "date-fns";
import { CalendarIcon, LogOut } from "lucide-react";
import { useState } from "react";
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from "recharts";
import { useRouter } from "next/navigation";

const riskData = [
  { name: "Jan", value: 1800 },
  { name: "Feb", value: 3600 },
  { name: "Mar", value: 3900 },
  { name: "Apr", value: 2500 },
  { name: "May", value: 3600 },
  { name: "Jun", value: 3900 },
  { name: "Jul", value: 2900 },
  { name: "Aug", value: 4700 },
  { name: "Sep", value: 2300 },
  { name: "Oct", value: 4100 },
  { name: "Nov", value: 3300 },
  { name: "Dec", value: 2200 },
];

const recentClaims = [
  { name: "Olivia Martin", email: "olivia.martin@email.com", amount: "$1999.00", path: "/person" },
  { name: "Jackson Lee", email: "jackson.lee@email.com", amount: "$39.00", path: "/claims/jackson-lee" },
  { name: "Isabella Nguyen", email: "isabella.nguyen@email.com", amount: "$299.00", path: "/claims/isabella-nguyen" },
  { name: "William Kim", email: "will@email.com", amount: "$99.00", path: "/claims/william-kim" },
  { name: "Sofia Davis", email: "sofia.davis@email.com", amount: "$39.00", path: "/claims/sofia-davis" },
];

export default function DashboardPage() {
  const [date, setDate] = useState<Date | undefined>(new Date());
  const router = useRouter();

  // Custom tooltip for the chart
  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-gray-800 p-3 rounded-md border border-gray-700">
          <p className="text-gray-300">{`${payload[0].payload.name}`}</p>
          <p className="text-blue-400 font-semibold">{`$${payload[0].value.toLocaleString()}`}</p>
        </div>
      );
    }
    return null;
  };

  // Handle click on claim
  const handleClaimClick = (path: string) => {
    router.push(path);
  };

  // Handle logout
    // Handle logout
    const handleLogout = () => {
      // Remove user from local storage
      localStorage.removeItem("user");
      // Redirect to home page
      router.push('/');
    };

  return (
    <div className="flex flex-col p-6 space-y-6 bg-black min-h-screen h-full text-white">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold">Dashboard</h2>
        <div className="flex space-x-2">
          <Popover>
            <PopoverTrigger asChild>
              <Button variant="outline" className="bg-gray-800 text-white border border-gray-700 hover:bg-gray-700">
                <CalendarIcon className="mr-2 h-4 w-4" />
                {date ? format(date, "PPP") : <span>Pick a date</span>}
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-auto p-0 bg-gray-800 border border-gray-700">
              <Calendar 
                mode="single" 
                selected={date} 
                onSelect={setDate} 
                initialFocus 
                className="bg-gray-800 text-white"
              />
            </PopoverContent>
          </Popover>
          <Button 
            variant="outline" 
            className="bg-gray-800 text-white border border-gray-700 hover:bg-gray-700"
            onClick={handleLogout}
          >
            <LogOut className="mr-2 h-4 w-4" />
            Logout
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
        <Card className="bg-gray-900 text-white border border-gray-800">
          <CardHeader className="pb-2">
            <CardTitle className="text-gray-300">Total Claim</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">$45,231.89</p>
            <p className="text-green-500 text-sm mt-1">+20.1% from last month</p>
          </CardContent>
        </Card>

        <Card className="bg-gray-900 text-white border border-gray-800">
          <CardHeader className="pb-2">
            <CardTitle className="text-gray-300">Active Users</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">+2,350</p>
            <p className="text-green-500 text-sm mt-1">+180.1% from last month</p>
          </CardContent>
        </Card>

        <Card className="bg-gray-900 text-white border border-gray-800">
          <CardHeader className="pb-2">
            <CardTitle className="text-gray-300">Interest</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">+12,234</p>
            <p className="text-green-500 text-sm mt-1">+19% from last month</p>
          </CardContent>
        </Card>

        <Card className="bg-gray-900 text-white border border-gray-800">
          <CardHeader className="pb-2">
            <CardTitle className="text-gray-300">Open Disputes</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">+573</p>
            <p className="text-green-500 text-sm mt-1">+201 since last hour</p>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 flex-grow">
        <Card className="bg-gray-900 text-white border border-gray-800 flex flex-col">
          <CardHeader>
            <CardTitle className="text-gray-300">Premium Overview</CardTitle>
          </CardHeader>
          <CardContent className="flex-grow">
            <div className="h-full min-h-[350px]">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart 
                  data={riskData} 
                  margin={{ top: 10, right: 10, left: 0, bottom: 20 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#333" vertical={false} />
                  <XAxis 
                    dataKey="name" 
                    stroke="#ccc" 
                    axisLine={{ stroke: '#444' }}
                    tickLine={{ stroke: '#444' }}
                  />
                  <YAxis 
                    stroke="#ccc" 
                    axisLine={{ stroke: '#444' }}
                    tickLine={{ stroke: '#444' }}
                    tickFormatter={(value) => `$${value}`}
                  />
                  <Tooltip content={<CustomTooltip />} />
                  <Bar 
                    dataKey="value" 
                    fill="#3b82f6" 
                    radius={[4, 4, 0, 0]}
                    animationDuration={1500}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        <Card className="bg-gray-900 text-white border border-gray-800 flex flex-col">
          <CardHeader>
            <CardTitle className="text-gray-300">Recent Claims</CardTitle>
            <p className="text-sm text-gray-400 mt-1">265 Claims this month</p>
          </CardHeader>
          <CardContent className="space-y-4 flex-grow">
            {recentClaims.map((claim, idx) => (
              <div 
                key={idx} 
                className="flex justify-between items-center border-b border-gray-700 pb-3 pt-1 cursor-pointer hover:bg-gray-800 rounded px-2 transition-colors"
                onClick={() => handleClaimClick(claim.path)}
              >
                <div>
                  <p className="font-semibold">{claim.name}</p>
                  <p className="text-sm text-gray-400">{claim.email}</p>
                </div>
                <span className="font-semibold text-blue-400">{claim.amount}</span>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}