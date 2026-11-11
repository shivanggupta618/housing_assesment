import type { Metadata } from 'next';
import Link from 'next/link';
import './globals.css';
export const metadata:Metadata={title:'Housing Price Assessment',description:'Housing market analysis and property value estimates'};
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="en"><body><header className="topbar"><nav aria-label="Main navigation"><Link href="/">Overview</Link><Link href="/estimator">Value estimate</Link><Link href="/market">Market data</Link></nav></header>{children}<footer>Housing price assessment <span>Estimates are informational and are not formal appraisals.</span></footer></body></html>}
