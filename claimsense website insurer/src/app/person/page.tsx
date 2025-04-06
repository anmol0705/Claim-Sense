// app/customers/[id]/page.tsx
'use client';

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { TabsContent, Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { CalendarIcon, FileText, AlertTriangle, Check, Clock, ChevronLeft, ChevronRight, Plus, Car } from "lucide-react";
import { format } from "date-fns";
import { useState, useRef } from "react";

// Sample customer data
const customerData = {
  id: "CUS-42891",
  name: "Olivia Martin",
  email: "olivia.martin@email.com",
  licenseNumber: "DL-789456123",
  policyNumber: "POL-2023-78942",
  riskScore: 73,
  phone: "(555) 123-4567",
  address: "1234 Main Street, San Francisco, CA 94107",
  joinDate: "2022-05-15",
};

// Sample vehicles data
const vehicles = [
  {
    id: "VEH-001",
    make: "Toyota",
    model: "Camry",
    year: "2022",
    licensePlate: "ABC123",
    vin: "1HGCM82633A004352",
    coverage: "Comprehensive",
    premium: "$1,240/year"
  },
  {
    id: "VEH-002",
    make: "Honda",
    model: "CR-V",
    year: "2021",
    licensePlate: "XYZ789",
    vin: "5FNRL6H58MB028424",
    coverage: "Liability",
    premium: "$980/year"
  }
];

// Sample claims data
const pastClaims = [
  { 
    id: "CLM-78942", 
    date: "2023-11-15", 
    type: "Collision", 
    amount: "$3,450.00", 
    status: "Completed", 
    description: "Front bumper damage from parking accident" 
  },
  { 
    id: "CLM-65432", 
    date: "2023-07-22", 
    type: "Comprehensive", 
    amount: "$780.00", 
    status: "Completed", 
    description: "Windshield replacement due to rock damage" 
  },
];

// Sample disputes data
const ongoingDisputes = [
  { 
    id: "DSP-12345", 
    date: "2024-01-18", 
    type: "Claim Amount", 
    status: "In Review", 
    description: "Customer disputes repair cost estimate" 
  },
];

export default function CustomerDetailsPage() {
  const [currentVehicle, setCurrentVehicle] = useState(0);
  const sliderRef = useRef<HTMLDivElement>(null);

  // Progress ring calculation
  const radius = 60;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (customerData.riskScore / 100) * circumference;
  
  // Get status badge style based on status
  const getStatusBadge = (status: string) => {
    switch(status) {
      case "Completed":
        return <Badge className="bg-green-600"><Check className="mr-1 h-3 w-3" /> {status}</Badge>;
      case "In Review":
        return <Badge className="bg-yellow-600"><Clock className="mr-1 h-3 w-3" /> {status}</Badge>;
      case "Pending Documentation":
        return <Badge className="bg-blue-600"><FileText className="mr-1 h-3 w-3" /> {status}</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  // Vehicle slider navigation
  const nextVehicle = () => {
    setCurrentVehicle((prev) => (prev + 1) % vehicles.length);
  };

  const prevVehicle = () => {
    setCurrentVehicle((prev) => (prev - 1 + vehicles.length) % vehicles.length);
  };

  return (
    <div className="flex flex-col bg-[#0A0C13] min-h-screen">
      {/* Header */}
      <div className="flex justify-between items-center p-4 px-6">
        <h2 className="text-xl font-semibold text-white">Customer Details</h2>
        <div className="flex items-center">
        <Button
          className="bg-[#141A2A] hover:bg-[#1E2640] text-gray-300"
          onClick={() => window.location.href = '/Dashboard'}
        >
          <ChevronLeft className="mr-2 h-4 w-4" />
          Dashboard
        </Button>
        </div>
      </div>

      {/* Main content - scrollable */}
      <div className="flex-1 p-4 px-6 overflow-y-auto">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
          {/* Top row - Stats summary cards */}
          <div className="lg:col-span-3">
            <Card className="bg-[#141A2A] border-0 shadow-md">
              <CardContent className="p-6">
                <div className="text-sm text-gray-400 mb-1">Policy Number</div>
                <div className="text-xl font-semibold text-white">{customerData.policyNumber}</div>
                <div className="text-green-500 text-sm mt-2">Active policy</div>
              </CardContent>
            </Card>
          </div>
          
          <div className="lg:col-span-3">
            <Card className="bg-[#141A2A] border-0 shadow-md">
              <CardContent className="p-6">
                <div className="text-sm text-gray-400 mb-1">Total Claims</div>
                <div className="text-xl font-semibold text-white">{pastClaims.length}</div>
                <div className="text-green-500 text-sm mt-2">No active claims</div>
              </CardContent>
            </Card>
          </div>
          
          <div className="lg:col-span-3">
            <Card className="bg-[#141A2A] border-0 shadow-md">
              <CardContent className="p-6">
                <div className="text-sm text-gray-400 mb-1">Annual Premium</div>
                <div className="text-xl font-semibold text-white">$2,220.00</div>
                <div className="text-green-500 text-sm mt-2">In good standing</div>
              </CardContent>
            </Card>
          </div>
          
          <div className="lg:col-span-3">
            <Card className="bg-[#141A2A] border-0 shadow-md">
              <CardContent className="p-6">
                <div className="text-sm text-gray-400 mb-1">Open Disputes</div>
                <div className="text-xl font-semibold text-white">{ongoingDisputes.length}</div>
                <div className="text-blue-500 text-sm mt-2">+1 since last month</div>
              </CardContent>
            </Card>
          </div>

          {/* Customer details & Risk score */}
          <div className="lg:col-span-8">
            <Card className="bg-[#141A2A] border-0 shadow-md">
              <CardHeader className="pb-0">
                <CardTitle className="text-lg font-semibold text-white">Personal Details</CardTitle>
              </CardHeader>
              <CardContent className="pt-4">
                <div className="flex items-center gap-4 mb-6">
                  <Avatar className="h-12 w-12 border border-blue-600">
                    <AvatarImage src="" />
                    <AvatarFallback className="text-lg bg-blue-600">{customerData.name.split(' ').map(n => n[0]).join('')}</AvatarFallback>
                  </Avatar>
                  <div>
                    <h3 className="font-medium text-lg text-white">{customerData.name}</h3>
                    <p className="text-sm text-gray-400">{customerData.id}</p>
                  </div>
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-y-6 gap-x-6">
                  <div>
                    <p className="text-sm text-gray-400 mb-1">License Number</p>
                    <p className="font-medium text-white">{customerData.licenseNumber}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-400 mb-1">Email</p>
                    <p className="font-medium text-white">{customerData.email}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-400 mb-1">Phone</p>
                    <p className="font-medium text-white">{customerData.phone}</p>
                  </div>
                  <div className="md:col-span-2">
                    <p className="text-sm text-gray-400 mb-1">Address</p>
                    <p className="font-medium text-white">{customerData.address}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
          
          <div className="lg:col-span-4">
            <Card className="bg-[#141A2A] border-0 shadow-md h-full">
              <CardHeader className="pb-0">
                <CardTitle className="text-lg font-semibold text-white">Risk Overview</CardTitle>
              </CardHeader>
              <CardContent className="flex flex-col items-center justify-center">
                {/* Progress ring */}
                <div className="relative flex items-center justify-center my-4">
                  <svg width="140" height="140" viewBox="0 0 150 150" className="transform -rotate-90">
                    {/* Background circle */}
                    <circle
                      cx="75"
                      cy="75"
                      r={radius}
                      fill="none"
                      stroke="#1E2640"
                      strokeWidth="10"
                    />
                    {/* Progress circle */}
                    <circle
                      cx="75"
                      cy="75"
                      r={radius}
                      fill="none"
                      stroke="#3B82F6"
                      strokeWidth="10"
                      strokeDasharray={circumference}
                      strokeDashoffset={strokeDashoffset}
                      strokeLinecap="round"
                    />
                  </svg>
                  {/* Risk score in the middle */}
                  <div className="absolute flex flex-col items-center justify-center">
                    <span className="text-3xl font-bold text-white">{customerData.riskScore}</span>
                    <span className="text-xs text-gray-400">Risk Score</span>
                  </div>
                </div>

                <Button className="w-full bg-blue-600 hover:bg-blue-700 mt-2">
                  <Car className="mr-2 h-4 w-4" /> Review Recent Trips
                </Button>
              </CardContent>
            </Card>
          </div>

          {/* Vehicles section */}
          <div className="lg:col-span-12">
            <Card className="bg-[#141A2A] border-0 shadow-md">
              <CardHeader className="pb-2 flex flex-row items-center justify-between">
                <CardTitle className="text-lg font-semibold text-white">Vehicles</CardTitle>
                
              </CardHeader>
              <CardContent>
                <div className="relative">
                  {/* Vehicle slider */}
                  <div ref={sliderRef} className="overflow-hidden">
                    <div 
                      className="flex transition-transform duration-300 ease-in-out"
                      style={{ transform: `translateX(-${currentVehicle * 100}%)` }}
                    >
                      {vehicles.map((vehicle) => (
                        <div key={vehicle.id} className="min-w-full">
                          <div className="flex flex-col md:flex-row gap-4">
                            
                            
                            {/* Vehicle details */}
                            <div className="flex-1">
                              <div className="flex justify-between mb-4">
                                <h3 className="font-medium text-lg text-white">{vehicle.make} {vehicle.model} ({vehicle.year})</h3>
                                <Badge className="bg-blue-600">{vehicle.id}</Badge>
                              </div>
                              
                              <div className="grid grid-cols-2 gap-y-4 gap-x-6">
                                <div>
                                  <p className="text-sm text-gray-400 mb-1">License Plate</p>
                                  <p className="text-white">{vehicle.licensePlate}</p>
                                </div>
                                <div>
                                  <p className="text-sm text-gray-400 mb-1">VIN</p>
                                  <p className="text-white">{vehicle.vin}</p>
                                </div>
                                <div>
                                  <p className="text-sm text-gray-400 mb-1">Coverage</p>
                                  <p className="text-white">{vehicle.coverage}</p>
                                </div>
                                <div>
                                  <p className="text-sm text-gray-400 mb-1">Premium</p>
                                  <p className="text-blue-500 font-medium">{vehicle.premium}</p>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                  
                  {/* Vehicle navigation arrows */}
                  {vehicles.length > 1 && (
                    <>
                      <button 
                        onClick={prevVehicle}
                        className="absolute left-0 top-1/2 -translate-y-1/2 bg-[#0A0C13] hover:bg-[#1E2640] rounded-full p-1 flex items-center justify-center z-10"
                      >
                        <ChevronLeft className="h-6 w-6" />
                      </button>
                      <button 
                        onClick={nextVehicle}
                        className="absolute right-0 top-1/2 -translate-y-1/2 bg-[#0A0C13] hover:bg-[#1E2640] rounded-full p-1 flex items-center justify-center z-10"
                      >
                        <ChevronRight className="h-6 w-6" />
                      </button>
                    </>
                  )}
                  
                  {/* Pagination dots */}
                  {vehicles.length > 1 && (
                    <div className="flex justify-center mt-4 gap-2">
                      {vehicles.map((_, idx) => (
                        <button 
                          key={idx} 
                          className={`h-2 w-2 rounded-full ${currentVehicle === idx ? 'bg-blue-600' : 'bg-[#1E2640]'}`}
                          onClick={() => setCurrentVehicle(idx)}
                        ></button>
                      ))}
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Claims and History */}
          <div className="lg:col-span-12">
            <Card className="bg-[#141A2A] border-0 shadow-md">
              <Tabs defaultValue="claims" className="w-full">
                <CardHeader className="pb-0">
                  <div className="flex items-center justify-between">
                    <CardTitle className="text-lg font-semibold text-white">Claims & Disputes</CardTitle>
                    <TabsList className="bg-[#0A0C13]">
                      <TabsTrigger value="claims" className="data-[state=active]:bg-blue-600">Claims</TabsTrigger>
                      <TabsTrigger value="disputes" className="data-[state=active]:bg-blue-600">Disputes</TabsTrigger>
                    </TabsList>
                  </div>
                </CardHeader>
                <CardContent className="pt-4">
                  <TabsContent value="claims" className="mt-0">
                    <div className="rounded border border-[#1E2640]">
                      <div className="relative w-full overflow-auto max-h-72">
                        <table className="w-full caption-bottom text-sm">
                          <thead className="border-b border-[#1E2640] sticky top-0 bg-[#141A2A]">
                            <tr>
                              <th className="h-12 px-4 text-left align-middle font-medium text-gray-400">ID</th>
                              <th className="h-12 px-4 text-left align-middle font-medium text-gray-400">Date</th>
                              <th className="h-12 px-4 text-left align-middle font-medium text-gray-400">Type</th>
                              <th className="h-12 px-4 text-left align-middle font-medium text-gray-400">Amount</th>
                              <th className="h-12 px-4 text-left align-middle font-medium text-gray-400">Status</th>
                            </tr>
                          </thead>
                          <tbody>
                            {pastClaims.map((claim) => (
                              <tr key={claim.id} className="border-b border-[#1E2640] hover:bg-[#1E2640]">
                                <td className="px-4 py-3 align-middle font-medium text-white">{claim.id}</td>
                                <td className="px-4 py-3 align-middle text-white">{format(new Date(claim.date), "MMM d, yyyy")}</td>
                                <td className="px-4 py-3 align-middle text-white">{claim.type}</td>
                                <td className="px-4 py-3 align-middle text-blue-500 font-medium">{claim.amount}</td>
                                <td className="px-4 py-3 align-middle">{getStatusBadge(claim.status)}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                    <div className="flex justify-center mt-4">
                      <Button className="bg-blue-600 hover:bg-blue-700">View All Claims</Button>
                    </div>
                  </TabsContent>
                  <TabsContent value="disputes" className="mt-0">
                    <div className="rounded border border-[#1E2640]">
                      <div className="relative w-full overflow-auto max-h-72">
                        <table className="w-full caption-bottom text-sm">
                          <thead className="border-b border-[#1E2640] sticky top-0 bg-[#141A2A]">
                            <tr>
                              <th className="h-12 px-4 text-left align-middle font-medium text-gray-400">ID</th>
                              <th className="h-12 px-4 text-left align-middle font-medium text-gray-400">Date</th>
                              <th className="h-12 px-4 text-left align-middle font-medium text-gray-400">Type</th>
                              <th className="h-12 px-4 text-left align-middle font-medium text-gray-400">Status</th>
                              <th className="h-12 px-4 text-left align-middle font-medium text-gray-400">Description</th>
                            </tr>
                          </thead>
                          <tbody>
                            {ongoingDisputes.map((dispute) => (
                              <tr key={dispute.id} className="border-b border-[#1E2640] hover:bg-[#1E2640]">
                                <td className="px-4 py-3 align-middle font-medium text-white">{dispute.id}</td>
                                <td className="px-4 py-3 align-middle text-white">{format(new Date(dispute.date), "MMM d, yyyy")}</td>
                                <td className="px-4 py-3 align-middle text-white">{dispute.type}</td>
                                <td className="px-4 py-3 align-middle">{getStatusBadge(dispute.status)}</td>
                                <td className="px-4 py-3 align-middle text-white">{dispute.description}</td>
                              </tr>
                            ))}
                            {ongoingDisputes.length === 0 && (
                              <tr>
                                <td colSpan={5} className="p-4 text-center text-gray-400">No ongoing disputes</td>
                              </tr>
                            )}
                          </tbody>
                        </table>
                      </div>
                    </div>
                    <div className="flex justify-center mt-4">
                      <Button className="bg-blue-600 hover:bg-blue-700">View All Disputes</Button>
                    </div>
                  </TabsContent>
                </CardContent>
              </Tabs>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}