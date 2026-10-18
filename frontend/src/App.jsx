import { useEffect, useMemo, useState } from 'react';
import { ShieldCheck, Search, Plus, ArrowRight, MapPin, Package, BookOpen, Laptop, KeyRound, Menu, X, BadgeCheck, Clock3, CircleUserRound, LogOut } from 'lucide-react';
import { deleteMyItem, deleteSecurityItem, emailReportOwner, getItems, getMyItems, getSecurityItems, getPendingClaims, loginStudent, logoutStudent, registerStudent, requestPasswordReset, resetPassword, resolveMyItem, reviewItem, reviewClaim, submitClaim, verifyStudentEmail, reportItem } from './services/api';
import './otp.css';

const categories=['Electronics','Bags & Accessories','Documents','Clothing','Keys','Other'];
const API_BASE=import.meta.env.VITE_API_URL||'http://localhost:8080/api';
const imageUrlFor=(url)=>url?.startsWith('http')?url:`${API_BASE.replace(/\/api\/?$/,'')}${url}`;
function App(){
 const [items,setItems]=useState([]), [query,setQuery]=useState(''), [category,setCategory]=useState('All categories'), [status,setStatus]=useState('All statuses');
 const [modal,setModal]=useState(''), [notice,setNotice]=useState(''), [busy,setBusy]=useState(false), [mobileMenu,setMobileMenu]=useState(false);
 const [authMode,setAuthMode]=useState('register'), [profileOpen,setProfileOpen]=useState(false);
 const [securityPage,setSecurityPage]=useState(()=>window.location.pathname==='/security');
 const [securityView,setSecurityView]=useState('reports');
 const [myReportsPage,setMyReportsPage]=useState(()=>window.location.pathname==='/my-reports');
 const [securityItems,setSecurityItems]=useState([]);
 const [pendingClaims,setPendingClaims]=useState([]);
 const [myItems,setMyItems]=useState([]);
 const [auth,setAuth]=useState({name:'',email:'',password:''});
 const [resetEmail,setResetEmail]=useState('');
 const [resetPasswordValue,setResetPasswordValue]=useState('');
 const [confirmPasswordValue,setConfirmPasswordValue]=useState('');
 const [resetToken]=useState(()=>new URLSearchParams(window.location.search).get('resetToken')||'');
 const [currentUser,setCurrentUser]=useState(()=>{try{return JSON.parse(sessionStorage.getItem('campus-find-user')||'null')}catch{return null}});
 const [verificationCode,setVerificationCode]=useState('');
 const [report,setReport]=useState({title:'',type:'LOST',category:'Electronics',date:'',location:'',description:''});
 const [claimDetails,setClaimDetails]=useState('');
 const [foundDate,setFoundDate]=useState('');
 const [foundLocation,setFoundLocation]=useState('');
 const [emailDraft,setEmailDraft]=useState({itemId:null,recipient:'',subject:'',body:''});
 const [reportImage,setReportImage]=useState(null), [imagePreview,setImagePreview]=useState('');
 useEffect(()=>{getItems().then(data=>setItems(data)).catch(err=>setNotice(err.message));},[]);
 useEffect(()=>{if(!reportImage){setImagePreview('');return}const preview=URL.createObjectURL(reportImage);setImagePreview(preview);return()=>URL.revokeObjectURL(preview)},[reportImage]);
 useEffect(()=>{const handlePopState=()=>{setSecurityPage(window.location.pathname==='/security');setMyReportsPage(window.location.pathname==='/my-reports')};window.addEventListener('popstate',handlePopState);return()=>window.removeEventListener('popstate',handlePopState);},[]);
 useEffect(()=>{if(securityPage&&currentUser?.role==='SECURITY'){if(securityView==='claims')loadPendingClaims();else loadSecurityItems();}},[securityPage,securityView,currentUser?.token]);
 useEffect(()=>{if(myReportsPage&&currentUser?.token)loadMyItems();},[myReportsPage,currentUser?.token]);
 const filtered=useMemo(()=>items.filter(i=>{
  const text=`${i.title} ${i.location} ${i.description} ${i.category}`.toLowerCase();
  const type=(i.type||'').toUpperCase(); const state=(i.status||'').toUpperCase();
  return text.includes(query.toLowerCase())&&(category==='All categories'||i.category===category)&&(status==='All statuses'||type===status.toUpperCase()||state===status.toUpperCase());
 }),[items,query,category,status]);
 const selectedItem=items.find(i=>String(i.id)===modal.split(':')[1]);
 const openAuth=(mode='login')=>{setAuthMode(mode);setModal('auth');setProfileOpen(false)};
 const openReport=()=>{if(!currentUser){openAuth();setNotice('Please sign in before reporting an item.');return}setModal('report')};
 function saveSession(user){sessionStorage.setItem('campus-find-user',JSON.stringify(user));setCurrentUser(user);setProfileOpen(false)}
 async function signOut(){const token=currentUser?.token;try{if(token)await logoutStudent(token);}catch(err){setNotice(err.message)}finally{sessionStorage.removeItem('campus-find-user');setCurrentUser(null);setProfileOpen(false);setSecurityPage(false);setMyReportsPage(false);window.history.pushState({},'', '/');setNotice('You have signed out.')}}
 function navigateSecurity(){window.history.pushState({},'', '/security');setSecurityPage(true);setSecurityView('reports');setMyReportsPage(false);setProfileOpen(false)}
 function navigateMyReports(){window.history.pushState({},'', '/my-reports');setSecurityPage(false);setMyReportsPage(true);setProfileOpen(false)}
 function navigateHome(){window.history.pushState({},'', '/');setSecurityPage(false);setMyReportsPage(false);getItems().then(setItems).catch(err=>setNotice(err.message))}
 async function loadSecurityItems(){if(!currentUser?.token)return;setBusy(true);try{setSecurityItems(await getSecurityItems(currentUser.token));}catch(err){setNotice(err.message);}finally{setBusy(false)}}
 async function loadPendingClaims(){if(!currentUser?.token)return;setBusy(true);try{setPendingClaims(await getPendingClaims(currentUser.token));}catch(err){setNotice(err.message);}finally{setBusy(false)}}
 async function loadMyItems(){if(!currentUser?.token)return;setBusy(true);try{setMyItems(await getMyItems(currentUser.token));}catch(err){setNotice(err.message);}finally{setBusy(false)}}
 async function moderateItem(itemId,decision){setBusy(true);setNotice('');try{await reviewItem(itemId,decision,currentUser.token);setSecurityItems(prev=>prev.map(item=>item.id===itemId?{...item,status:decision}:item));setNotice(decision==='APPROVED'?'Report approved and published.':'Report rejected.');}catch(err){setNotice(err.message)}finally{setBusy(false)}}
 async function moderateClaim(claimId,decision){setBusy(true);setNotice('');try{const result=await reviewClaim(claimId,decision,currentUser.token);setPendingClaims(prev=>prev.filter(claim=>claim.id!==claimId));setNotice(result.message);}catch(err){setNotice(err.message)}finally{setBusy(false)}}
 async function deleteSecurityReport(itemId){if(!window.confirm('Delete this report and its linked submissions? This cannot be undone.'))return;setBusy(true);setNotice('');try{await deleteSecurityItem(itemId,currentUser.token);setSecurityItems(prev=>prev.filter(item=>item.id!==itemId));setNotice('Report deleted.');}catch(err){setNotice(err.message)}finally{setBusy(false)}}
 function openSecurityEmail(item){setEmailDraft({itemId:item.id,recipient:item.reporterEmail,subject:`Campus Find update: ${item.title}`,body:`Hello ${item.reporter||'there'},\n\nCampus Security has an update about your ${item.type.toLowerCase()} report "${item.title}".\n\n`});setModal('security-email')}
 async function sendSecurityEmail(event){event.preventDefault();if(!emailDraft.itemId)return;setBusy(true);setNotice('');try{const result=await emailReportOwner(emailDraft.itemId,{subject:emailDraft.subject,body:emailDraft.body},currentUser.token);setModal('');setNotice(result.message)}catch(err){setNotice(err.message)}finally{setBusy(false)}}
 async function resolveMyReport(itemId){setBusy(true);setNotice('');try{await resolveMyItem(itemId,currentUser.token);setMyItems(prev=>prev.map(item=>item.id===itemId?{...item,status:'RESOLVED'}:item));setItems(await getItems());setNotice('Report marked as resolved and removed from the public board.');}catch(err){setNotice(err.message)}finally{setBusy(false)}}
 async function deleteMyReport(itemId){if(!window.confirm('Delete this report? This cannot be undone.'))return;setBusy(true);setNotice('');try{await deleteMyItem(itemId,currentUser.token);setMyItems(prev=>prev.filter(item=>item.id!==itemId));setItems(await getItems());setNotice('Your report was deleted.');}catch(err){setNotice(err.message)}finally{setBusy(false)}}
 async function submitAuth(e){e.preventDefault();setBusy(true);setNotice('');try{if(authMode==='register'){await registerStudent(auth);setVerificationCode('');setModal('otp');}else{const user=await loginStudent({email:auth.email,password:auth.password});saveSession(user);setNotice(`Welcome back, ${user.name}.`);setModal('');}}catch(err){setNotice(err.message)}finally{setBusy(false)}}
 async function submitVerification(e){e.preventDefault();setBusy(true);setNotice('');try{const user=await verifyStudentEmail(auth.email,verificationCode.trim());saveSession(user);setNotice(user.message);setModal('');}catch(err){setNotice(err.message)}finally{setBusy(false)}}
 async function submitPasswordResetRequest(e){e.preventDefault();setBusy(true);setNotice('');try{const result=await requestPasswordReset(resetEmail.trim());setModal('');setNotice(result.message);}catch(err){setNotice(err.message)}finally{setBusy(false)}}
 async function submitPasswordReset(e){e.preventDefault();if(resetPasswordValue!==confirmPasswordValue){setNotice('The passwords do not match.');return}setBusy(true);setNotice('');try{const result=await resetPassword(resetToken,resetPasswordValue);window.history.replaceState({},document.title,window.location.pathname);setNotice(result.message);setResetPasswordValue('');setConfirmPasswordValue('');setTimeout(()=>{window.location.href='/';},1200);}catch(err){setNotice(err.message)}finally{setBusy(false)}}
 function openClaim(item){if(!item)return;if(!currentUser){openAuth();setNotice('Please sign in before sending private verification details.');return}setClaimDetails('');setFoundDate('');setFoundLocation('');setModal(`claim:${item.id}`)}
 async function submitClaimForm(e){e.preventDefault();if(!selectedItem)return;const kind=selectedItem.type==='FOUND'?'CLAIM':'FOUND_NOTICE';const payload={itemId:selectedItem.id,kind,verificationDetails:claimDetails.trim()};if(kind==='FOUND_NOTICE'){payload.foundDate=foundDate;payload.foundLocation=foundLocation.trim()}setBusy(true);setNotice('');try{const result=await submitClaim(payload,currentUser.token);setModal('');setNotice(result.message)}catch(err){setNotice(err.message)}finally{setBusy(false)}}
 function selectReportImage(file){if(!file){setReportImage(null);return}if(!['image/jpeg','image/png'].includes(file.type)){setNotice('Choose a JPEG or PNG image.');return}if(file.size>5*1024*1024){setNotice('Image must be 5 MB or smaller.');return}setNotice('');setReportImage(file)}
 async function submitReport(e){e.preventDefault();setBusy(true);setNotice('');try{await reportItem(report,currentUser.token,reportImage);setItems(await getItems());setReportImage(null);setReport({title:'',type:'LOST',category:'Electronics',date:'',location:'',description:''});setNotice('Report saved and submitted for campus security review. It will appear after approval.');setModal('');}catch(err){setNotice(err.message)}finally{setBusy(false)}}
 if(resetToken)return <main className="reset-page"><a className="brand reset-brand" href="/"><span className="brand-mark"><ShieldCheck size={25}/></span><span><b>Campus Find</b><small>MES · LOST & FOUND</small></span></a><section className="reset-card"><p className="eyebrow">ACCOUNT SECURITY</p><h1>Choose a new password</h1><p className="reset-description">Create a new password for your Campus Find account.</p><form onSubmit={submitPasswordReset}><label>New password<input type="password" minLength="8" required autoComplete="new-password" value={resetPasswordValue} onChange={e=>setResetPasswordValue(e.target.value)} placeholder="At least 8 characters"/></label><label>Confirm new password<input type="password" minLength="8" required autoComplete="new-password" value={confirmPasswordValue} onChange={e=>setConfirmPasswordValue(e.target.value)} placeholder="Enter the new password again"/></label><button className="btn btn-dark full" disabled={busy}>{busy?'Saving…':'Save new password'} <ArrowRight size={16}/></button></form></section>{notice&&<div className="toast" role="status">{notice}<button onClick={()=>setNotice('')}>×</button></div>}</main>;
 if(securityPage)return <main className="security-page">
  <header className="security-header"><a className="brand" href="/" onClick={e=>{e.preventDefault();navigateHome()}}><span className="brand-mark"><ShieldCheck size={25}/></span><span><b>Campus Find</b><small>CAMPUS SECURITY</small></span></a><button className="btn btn-outline" onClick={()=>{navigateHome();setNotice('')}}>Back to board</button></header>
  <section className="security-content">
   <p className="eyebrow">CAMPUS SECURITY · MODERATION</p><h1>{securityView==='claims'?'Claims and found notices':'Report management'}</h1><p className="security-intro">Review reports across all statuses. Compose email to the report owner or remove a report when needed. Report-owner details are visible only to Campus Security.</p>
   <div className="security-toolbar"><div className="security-tabs"><button className={securityView==='reports'?'active':''} onClick={()=>setSecurityView('reports')}>Reports</button><button className={securityView==='claims'?'active':''} onClick={()=>setSecurityView('claims')}>Claims and notices</button></div><span>{securityView==='claims'?pendingClaims.length:securityItems.length} {securityView==='claims'?'pending submission':'total report'}{(securityView==='claims'?pendingClaims.length:securityItems.length)===1?'':'s'}</span><button className="btn btn-outline" disabled={busy} onClick={securityView==='claims'?loadPendingClaims:loadSecurityItems}>Refresh</button></div>
   {!currentUser?.token||currentUser.role!=='SECURITY'?<div className="security-empty"><ShieldCheck size={32}/><h2>Security access required</h2><p>Sign in using the configured campus security account to review reports.</p><button className="btn btn-dark" onClick={()=>{navigateHome();openAuth('login')}}>Sign in</button></div>
   :securityView==='claims'?pendingClaims.length===0?<div className="security-empty"><BadgeCheck size={32}/><h2>All caught up</h2><p>There are no pending claims or found notices.</p></div>
   :<div className="security-list">{pendingClaims.map(claim=><article className="security-card" key={claim.id}>
    <div className="security-card-heading"><div><span className="security-type">{claim.kind==='CLAIM'?'OWNERSHIP CLAIM':'FOUND NOTICE'} · {claim.item?.type}</span><h2>{claim.item?.title}</h2></div><span className="security-date">{claim.createdAt?.slice(0,10)}</span></div>
    <p className="security-location"><CircleUserRound size={15}/>{claim.claimantName} · {claim.claimantEmail}</p>
    {claim.kind==='FOUND_NOTICE'&&<p className="security-location"><MapPin size={15}/>Found {claim.foundDate} · {claim.foundLocation}</p>}
    <p className="security-description"><strong>Private verification details:</strong> {claim.verificationDetails}</p>
    <div className="security-card-footer"><span>Approval emails both parties to arrange handoff at Campus Security.</span><div><button className="btn btn-reject" disabled={busy} onClick={()=>moderateClaim(claim.id,'REJECTED')}>Reject</button><button className="btn btn-approve" disabled={busy} onClick={()=>moderateClaim(claim.id,'APPROVED')}>Approve & notify</button></div></div>
   </article>)}</div>
   :securityItems.length===0?<div className="security-empty"><BadgeCheck size={32}/><h2>No reports found</h2><p>There are no reports in the system yet.</p></div>
   :<div className="security-list">{securityItems.map(item=><article className="security-card" key={item.id}>
    {item.imageUrl&&<img className="security-item-image" src={imageUrlFor(item.imageUrl)} alt={item.title}/>}
    <div className="security-card-heading"><div><span className="security-type">{item.type} · {item.category} · {item.status}</span><h2>{item.title}</h2></div><span className="security-date">{item.date}</span></div>
    <p className="security-location"><MapPin size={15}/>{item.location}</p><p className="security-description">{item.description}</p>
    <div className="security-card-footer"><span>Reported by {item.reporter||'MES student'}{item.reporterEmail?<><br/>{item.reporterEmail}</>:<><br/>No saved owner email address</>}</span><div>
     {item.reporterEmail&&<button className="btn btn-outline" disabled={busy} onClick={()=>openSecurityEmail(item)}>Compose email</button>}
     {item.status==='PENDING'&&<><button className="btn btn-reject" disabled={busy} onClick={()=>moderateItem(item.id,'REJECTED')}>Reject</button><button className="btn btn-approve" disabled={busy} onClick={()=>moderateItem(item.id,'APPROVED')}>Approve</button></>}
     <button className="btn btn-reject" disabled={busy} onClick={()=>deleteSecurityReport(item.id)}>Delete report</button>
    </div></div>
   </article>)}</div>}
  </section>{notice&&<div className="toast" role="status">{notice}<button onClick={()=>setNotice('')}>×</button></div>}
 </main>;
 if(myReportsPage)return <main className="security-page">
  <header className="security-header"><a className="brand" href="/" onClick={e=>{e.preventDefault();navigateHome()}}><span className="brand-mark"><ShieldCheck size={25}/></span><span><b>Campus Find</b><small>MY REPORTS</small></span></a><button className="btn btn-outline" onClick={navigateHome}>Back to board</button></header>
  <section className="security-content">
   <p className="eyebrow">YOUR CAMPUS REPORTS</p><h1>My reports</h1><p className="security-intro">Track your reports, mark approved items as resolved, or delete a report you no longer need.</p>
   <div className="security-toolbar"><span>{myItems.length} report{myItems.length===1?'':'s'}</span><button className="btn btn-outline" disabled={busy} onClick={loadMyItems}>Refresh</button></div>
   {!currentUser?.token?<div className="security-empty"><CircleUserRound size={32}/><h2>Sign in to view your reports</h2><p>Your reports are private to your account.</p><button className="btn btn-dark" onClick={()=>{navigateHome();openAuth('login')}}>Sign in</button></div>
   :myItems.length===0?<div className="security-empty"><Package size={32}/><h2>No linked reports yet</h2><p>New reports are saved to your account. Older reports created before account ownership was recorded may not be linked here; contact Campus Security to verify ownership.</p></div>:<div className="security-list">{myItems.map(item=><article className="security-card" key={item.id}>
    {item.imageUrl&&<img className="security-item-image" src={imageUrlFor(item.imageUrl)} alt={item.title}/>}
    <div className="security-card-heading"><div><span className="security-type">{item.type} · {item.category} · {item.status}</span><h2>{item.title}</h2></div><span className="security-date">{item.date}</span></div>
    <p className="security-location"><MapPin size={15}/>{item.location}</p><p className="security-description">{item.description}</p>
    <div className="security-card-footer"><span>{item.status==='PENDING'?'Waiting for campus security review':item.status==='RESOLVED'?'Marked as resolved':'Published on the board'}</span><div>{item.status==='APPROVED'&&<button className="btn btn-approve" disabled={busy} onClick={()=>resolveMyReport(item.id)}>Mark resolved</button>}<button className="btn btn-reject" disabled={busy} onClick={()=>deleteMyReport(item.id)}>Delete report</button></div></div>
   </article>)}</div>}
  </section>{notice&&<div className="toast" role="status">{notice}<button onClick={()=>setNotice('')}>×</button></div>}
 </main>;
 return <>
  <header className="nav"><a className="brand" href="#top"><span className="brand-mark"><ShieldCheck size={25}/></span><span><b>Campus Find</b><small>MES · LOST & FOUND</small></span></a><nav className={mobileMenu?'nav-links show':'nav-links'}><a href="#board" onClick={()=>setMobileMenu(false)}>Browse board</a><a href="#how-it-works" onClick={()=>setMobileMenu(false)}>How it works</a></nav><div className="nav-actions"><button className="btn btn-orange" onClick={openReport}><Plus size={17}/> Report item</button>{currentUser?<div className="profile-wrap"><button className="profile-button" aria-label="Open profile menu" aria-expanded={profileOpen} onClick={()=>setProfileOpen(!profileOpen)}><CircleUserRound size={21}/></button>{profileOpen&&<div className="profile-menu"><strong>{currentUser.name}</strong><span>{currentUser.email}</span><button onClick={navigateMyReports}><Package size={15}/> My reports</button>{currentUser.role==='SECURITY'&&<button onClick={navigateSecurity}><ShieldCheck size={15}/> Campus security approvals</button>}<button onClick={signOut}><LogOut size={15}/> Sign out</button></div>}</div>:<button className="btn btn-outline" onClick={()=>openAuth('login')}><CircleUserRound size={16}/> Sign in</button>}</div><button className="mobile-toggle" onClick={()=>setMobileMenu(!mobileMenu)} aria-label="Toggle menu">{mobileMenu?<X/>:<Menu/>}</button></header>
  <main id="top"><section className="hero wrap"><div className="hero-copy"><p className="eyebrow"><span>✧</span> A BETTER WAY BACK TO YOUR THINGS</p><h1>Lost something?<br/><em>Start here.</em></h1><p className="hero-desc">A trusted campus board for the moments that matter. Browse what has been found, report what is missing, and let the MES community close the loop.</p><div className="hero-actions"><a className="btn btn-dark" href="#board">Browse the board <ArrowRight size={17}/></a><button className="btn btn-plain" onClick={openReport}>Report lost or found <Plus size={17}/></button></div></div><aside className="poster"><div className="tape"/><BookOpen size={24}/><h3>Run by students.<br/><strong>Looked after by campus security.</strong></h3><div className="poster-line"/><small>MUMBAI · MES CAMPUS<br/>Community board / 2026</small></aside></section>
  <section className="stats wrap"><div><b>{items.length}</b><span>approved listings</span></div><div><b>0</b><span>your pending reports</span></div><div><b>100%</b><span>college verified</span></div><p>The quickest route to the security desk starts with a clear description.</p></section>
  <section className="board wrap" id="board"><div className="section-head"><div><p className="eyebrow">THE COMMUNITY BOARD</p><h2>Recently reported</h2><p>Every listing is reviewed by Campus Security before it goes live.</p></div><span className="verified"><BadgeCheck size={16}/> Verified campus emails only</span></div><div className="filters"><label className="search"><Search size={19}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Search item, place, or detail..."/></label><label className="select-wrap"><span>⚑</span><select value={category} onChange={e=>setCategory(e.target.value)}><option>All categories</option>{categories.map(c=><option key={c}>{c}</option>)}</select></label><label className="select-wrap"><select value={status} onChange={e=>setStatus(e.target.value)}><option>All statuses</option><option value="LOST">Lost</option><option value="FOUND">Found</option><option value="RESOLVED">Resolved</option><option value="PENDING">Pending</option></select></label></div>
  {filtered.length?<div className="item-grid">{filtered.map((item,idx)=><article className="item-card" key={item.id||item.title}><div className={`item-image ${idx%2?'laptop-image':'bag-image'}`}>{item.imageUrl?<img src={imageUrlFor(item.imageUrl)} alt={item.title}/>:idx%2?<Laptop size={74}/>:<Package size={74}/>}<span className={`status-pill ${(item.type||'FOUND').toLowerCase()}`}>{item.type||'FOUND'}</span></div><div className="item-body"><div className="item-meta"><span>{item.category}</span><span>{item.date||'Recently'}</span></div><h3>{item.title}</h3><p className="location"><MapPin size={15}/>{item.location}</p><p className="description">{item.description}</p><div className="item-footer"><span><BadgeCheck size={14}/>{item.reporter||'MES Student'}</span><button onClick={()=>setModal(`details:${item.id}`)}>View details</button></div><button className="claim-btn" onClick={()=>openClaim(item)}>{item.type==='FOUND'?'Submit a claim':'I found this item'} <ArrowRight size={16}/></button></div></article>)}</div>:<div className="empty"><Package size={34}/><h3>No matches yet.</h3><p>Try a broader search, or be the first to report this kind of item.</p></div>}</section>
  <section className="steps wrap" id="how-it-works"><div className="steps-intro"><p className="eyebrow">THREE SMALL STEPS</p><h2>Make the handoff<br/>easy.</h2><p>Good reports help the right owner recognise an item quickly. Keep details specific, and the security desk does the careful part.</p></div><div className="step"><small>01</small><BookOpen/><h3>Report clearly</h3><p>Add where, when, and the details only the owner would know.</p></div><div className="step"><small>02</small><ShieldCheck/><h3>We review</h3><p>Campus Security checks every post and claim before connecting people.</p></div><div className="step"><small>03</small><Package/><h3>Close the loop</h3><p>Meet safely at the desk and mark the item resolved.</p></div></section></main>
  <footer><span>CAMPUS FIND / MES CAMPUS</span><span>NEED HELP? VISIT THE CAMPUS SECURITY DESK.</span><span>A STUDENT-BUILT COMMUNITY BOARD · 2026</span></footer>
  {notice&&<div className="toast" role="status">{notice}<button onClick={()=>setNotice('')}>×</button></div>}
  {modal&&<div className="modal-backdrop" onMouseDown={e=>{if(e.target===e.currentTarget)setModal('')}}>
   <section className="modal">
    <button className="close" onClick={()=>setModal('')} aria-label="Close">×</button>
    {modal==='security-email'?<>
     <p className="eyebrow">CAMPUS SECURITY · EMAIL</p><h2>Compose report update</h2>
     <p className="otp-hint">This message will be sent only to the saved email address for this report: <strong>{emailDraft.recipient}</strong>. It will be sent as plain text.</p>
     <form onSubmit={sendSecurityEmail} className="claim-form">
      <label>Subject<input required maxLength="200" value={emailDraft.subject} onChange={e=>setEmailDraft({...emailDraft,subject:e.target.value})}/></label>
      <label>Message<textarea required maxLength="5000" rows="7" value={emailDraft.body} onChange={e=>setEmailDraft({...emailDraft,body:e.target.value})}/></label>
      <div className="form-actions"><button type="button" className="btn btn-outline" onClick={()=>setModal('')}>Cancel</button><button className="btn btn-dark" disabled={busy}>{busy?'Sending…':'Send email'} <ArrowRight size={16}/></button></div>
     </form>
    </>:modal==='forgot-password'?<>
     <p className="eyebrow">ACCOUNT RECOVERY</p><h2>Reset your password</h2>
     <p className="otp-hint">Enter the email address on your verified Campus Find account. If an account exists, we’ll email you a reset link.</p>
     <form onSubmit={submitPasswordResetRequest}><label>Email address<input required type="email" value={resetEmail} onChange={e=>setResetEmail(e.target.value)} placeholder="name_surname@student.mes.ac.in"/></label><button className="btn btn-dark full" disabled={busy}>{busy?'Sending link…':'Send reset link'} <ArrowRight size={16}/></button></form>
    </>:modal==='auth'?<>
     <p className="eyebrow">MES COLLEGE ACCESS</p><h2>{authMode==='register'?'Create your account':'Sign in to continue'}</h2>
     <div className="auth-tabs"><button type="button" className={authMode==='register'?'active':''} onClick={()=>setAuthMode('register')}>Create account</button><button type="button" className={authMode==='login'?'active':''} onClick={()=>setAuthMode('login')}>Sign in</button></div>
     <form onSubmit={submitAuth}>
      {authMode==='register'&&<label>Full name<input required value={auth.name} onChange={e=>setAuth({...auth,name:e.target.value})} placeholder="Your full name"/></label>}
      <label>College email<input required type="email" value={auth.email} onChange={e=>setAuth({...auth,email:e.target.value})} placeholder="name_surname2024_H01_cs@student.mes.ac.in"/></label>
      <label>Password<input required minLength="8" type="password" value={auth.password} onChange={e=>setAuth({...auth,password:e.target.value})} placeholder="At least 8 characters"/></label>
      {authMode==='login'&&<button type="button" className="forgot-password-link" onClick={()=>{setResetEmail(auth.email);setModal('forgot-password')}}>Forgot password?</button>}
      {authMode==='register'&&<p className="hint"><KeyRound size={14}/> Use a verified MES email or the configured Campus Security email.</p>}
      <button className="btn btn-dark full" disabled={busy}>{busy?'Please wait…':authMode==='register'?'Create account':'Sign in'} <ArrowRight size={16}/></button>
     </form>
    </>:modal==='otp'?<>
     <p className="eyebrow">EMAIL VERIFICATION</p><h2>Enter your verification code</h2>
     <p className="otp-hint">We sent a 6-digit code to <strong>{auth.email}</strong>. The code expires in 10 minutes.</p>
     <form onSubmit={submitVerification} className="otp-form"><label htmlFor="verification-code">Verification code</label><input id="verification-code" type="text" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength="6" required value={verificationCode} onChange={e=>setVerificationCode(e.target.value.replace(/\D/g,'').slice(0,6))} placeholder="000000"/><button className="btn btn-dark full" disabled={busy||verificationCode.length!==6}>{busy?'Verifying…':'Verify email'} <ArrowRight size={16}/></button></form>
    </>:modal==='report'?<>
     <p className="eyebrow">NEW CAMPUS RECORD</p><h2>Report an item</h2>
     <form onSubmit={submitReport} className="report-form">
      <label>What should we call it?<input required value={report.title} onChange={e=>setReport({...report,title:e.target.value})} placeholder="Black water bottle with stickers"/></label>
      <label>Record type<select value={report.type} onChange={e=>setReport({...report,type:e.target.value})}><option value="LOST">I lost this</option><option value="FOUND">I found this</option></select></label>
      <label>Category<select value={report.category} onChange={e=>setReport({...report,category:e.target.value})}>{categories.map(c=><option key={c}>{c}</option>)}</select></label>
      <label>Date<input type="date" required value={report.date} onChange={e=>setReport({...report,date:e.target.value})}/></label>
      <label className="wide">Where was it last seen?<input required value={report.location} onChange={e=>setReport({...report,location:e.target.value})} placeholder="Building, floor, or landmark"/></label>
      <label className="wide">Details for verification<textarea required rows="3" value={report.description} onChange={e=>setReport({...report,description:e.target.value})} placeholder="Add unique marks, contents, or where it was found…"/></label>
      <div className="wide image-picker">
       <label className="image-picker-control">Add a photo (optional)<input type="file" accept="image/png,image/jpeg" onChange={e=>selectReportImage(e.target.files?.[0]||null)}/><span>Choose JPEG or PNG · Max 5 MB</span></label>
       {imagePreview&&<div className="image-preview"><img src={imagePreview} alt="Selected item preview"/><button type="button" className="btn btn-outline" onClick={()=>setReportImage(null)}>Remove photo</button></div>}
      </div>
      <div className="wide form-actions"><button type="button" className="btn btn-outline" onClick={()=>setModal('')}>Cancel</button><button className="btn btn-dark" disabled={busy}>{busy?'Sending…':'Send for review'} <ArrowRight size={16}/></button></div>
     </form>
    </>:modal.startsWith('claim:')?<>
     <p className="eyebrow">{selectedItem?.type==='FOUND'?'ITEM OWNERSHIP CLAIM':'FOUND-ITEM NOTICE'}</p><h2>{selectedItem?.type==='FOUND'?'Submit a claim':'I found this item'} · {selectedItem?.title}</h2>
     <p className="otp-hint">{selectedItem?.type==='FOUND'?'Describe details that prove this item belongs to you, such as unique marks, contents, or purchase details.':'Tell Campus Security when and where you found it, plus private details that may help match it to this report.'} Campus Security reviews this privately. Do not include passwords, verification codes, or sensitive identity numbers.</p>
     <form onSubmit={submitClaimForm} className="claim-form">
      {selectedItem?.type==='LOST'&&<><label>Date found<input type="date" required max={new Date().toISOString().slice(0,10)} value={foundDate} onChange={e=>setFoundDate(e.target.value)}/></label><label>Where did you find it?<input required maxLength="180" value={foundLocation} onChange={e=>setFoundLocation(e.target.value)} placeholder="Building, floor, or campus location"/></label></>}
      <label>{selectedItem?.type==='FOUND'?'How can you verify ownership?':'Private details about the found item'}<textarea required minLength="20" maxLength="2000" rows="4" value={claimDetails} onChange={e=>setClaimDetails(e.target.value)} placeholder={selectedItem?.type==='FOUND'?'Describe unique identifying details known to the owner…':'Describe the item condition, distinctive features, or other useful matching details…'}/></label>
      <p className="claim-privacy-note">Your details are shared only with Campus Security during review. If approved, both parties receive an email to arrange handoff at the Campus Security desk.</p>
      <div className="form-actions"><button type="button" className="btn btn-outline" onClick={()=>setModal('')}>Cancel</button><button className="btn btn-dark" disabled={busy}>{busy?'Sending…':'Send for review'} <ArrowRight size={16}/></button></div>
     </form>
    </>:<>
     <p className="eyebrow">ITEM DETAILS</p><h2>{selectedItem?.title||'Listing details'}</h2>
     <p className="hero-desc">{selectedItem?.type==='FOUND'?'If this item belongs to you, send Campus Security private ownership-verification details.':'If you found this item, send Campus Security when and where you found it with matching details.'}</p>
     {selectedItem&&<button className="btn btn-dark full" onClick={()=>openClaim(selectedItem)}>{selectedItem.type==='FOUND'?'Submit a claim':'I found this item'} <ArrowRight size={16}/></button>}
    </>}
   </section>
  </div>}
 </>;
}
export default App;