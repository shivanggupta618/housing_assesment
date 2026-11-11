'use client';
import { useCallback, useState } from 'react';
export function useApi<T>() {
  const [data,setData]=useState<T|null>(null); const [error,setError]=useState(''); const [loading,setLoading]=useState(false);
  const run=useCallback(async(url:string,init?:RequestInit)=>{setLoading(true);setError('');try{const r=await fetch(url,init);const body=await r.json().catch(()=>({}));if(!r.ok)throw new Error(body.detail||body.error||`Request failed (${r.status})`);setData(body as T);return body as T;}catch(e){const m=e instanceof Error?e.message:'Unable to load data';setError(m);throw e;}finally{setLoading(false);}},[]);
  return {data,error,loading,run,setData};
}
