package com.host1syan.claudecn.cache;

import com.host1syan.claudecn.model.ServerConfig;

public class Injector {
    private static final String MARKER = "<!--claudecn-inject-->";

    public static String inject(String str, ServerConfig serverConfig) {
        if (str == null || str.isEmpty() || str.contains(MARKER)) {
            return str;
        }
        String str2 = MARKER + buildScript(serverConfig);
        int indexOfIgnoreCase = indexOfIgnoreCase(str, "<head>");
        if (indexOfIgnoreCase >= 0) {
            int i = indexOfIgnoreCase + 6;
            return str.substring(0, i) + str2 + str.substring(i);
        }
        return str2 + str;
    }

    private static String buildScript(ServerConfig serverConfig) {
        return "<script>(function(){var T=" + jsStr((serverConfig == null || serverConfig.token == null) ? "" : serverConfig.token) + ";function withAuth(h){try{  if(!h) h={};  if(T){ if(typeof h.has==='function'&&typeof h.set==='function'){ if(!h.has('Authorization')) h.set('Authorization','Bearer '+T); }         else if(Object.prototype.toString.call(h)==='[object Array]'){ var has=false; for(var i=0;i<h.length;i++){ if(String(h[i][0]).toLowerCase()==='authorization') has=true; } if(!has) h.push(['Authorization','Bearer '+T]); }         else { var found=false; for(var k in h){ if(k.toLowerCase()==='authorization') found=true; } if(!found) h['Authorization']='Bearer '+T; } }}catch(e){} return h;}var of=window.fetch;if(of){window.fetch=function(input,init){  try{    if(typeof input==='string'||input instanceof URL){      init=init||{}; init.headers=withAuth(init.headers);    } else if(input&&input.headers!==undefined){      var src=init&&init.headers!==undefined?init.headers:input.headers;      init=init||{};      init.headers=withAuth(new Headers(src));    }  }catch(e){}  return of.call(this,input,init);};}var oo=XMLHttpRequest.prototype.open;XMLHttpRequest.prototype.open=function(){  this._cnAuth=false;  return oo.apply(this,arguments);};var osh=XMLHttpRequest.prototype.setRequestHeader;XMLHttpRequest.prototype.setRequestHeader=function(k,v){  try{ if(String(k).toLowerCase()==='authorization') this._cnAuth=true; }catch(e){}  return osh.apply(this,arguments);};var os=XMLHttpRequest.prototype.send;XMLHttpRequest.prototype.send=function(){  try{    if(T&&!this._cnAuth){ this.setRequestHeader('Authorization','Bearer '+T); }  }catch(e){}  return os.apply(this,arguments);};var CN={},S={},domRun=false,domAtt=false;var cnPush=function(){  var r=domRun,a=domAtt;  for(var k in S){ if(S[k].run) r=true; if(S[k].att) a=true; }  try{ if(window.ClaudecnHost&&window.ClaudecnHost.status) window.ClaudecnHost.status(r?1:0,a?1:0); }catch(e){}};var cnMsg=function(sid,raw){  var m; try{ m=JSON.parse(raw); }catch(e){ return; }  if(!m||typeof m!=='object') return;  var st=S[sid]||(S[sid]={run:false,att:false,turn:false,bg:{},pend:{}});  var t=m.type;  if(t==='session_state'){ st.turn=(m.turnState==='running'); if(!st.turn) st.pend={}; }  else if(t==='status'){ st.turn=(m.state!=='idle'); }  else if(t==='permission_request'||t==='computer_use_permission_request'){    if(m.requestId) st.pend[m.requestId]=1; st.turn=true; }  else if(t==='permission_resolved'){    if(m.requestId) delete st.pend[m.requestId];    var left=false; for(var q in st.pend){ left=true; break; }    if(m.allowed===false&&!left) st.turn=false; }  else if(t==='permission_requests_snapshot'){    st.pend={};    var i,ids=m.toolRequestIds||[],ids2=m.computerUseRequestIds||[];    for(i=0;i<ids.length;i++) st.pend[ids[i]]=1;    for(i=0;i<ids2.length;i++) st.pend[ids2[i]]=1;    st.turn=!!m.turnActive; }  else if(t==='message_complete'){ st.turn=false; st.pend={}; }  else if(t==='content_start'||t==='content_delta'||t==='thinking'       ||t==='api_retry'||t==='streaming_fallback'       ||t==='tool_use_complete'||t==='tool_result'){ st.turn=true; }  else if(t==='system_notification'){    var d=m.data||{};    var id=d.task_id||d.taskId||d.tool_use_id||d.toolUseId;    if(m.subtype==='task_started'||m.subtype==='task_progress'){ if(id) st.bg[id]=1; }    else if(m.subtype==='task_notification'){ if(id) delete st.bg[id]; }    else if(m.subtype==='session_cleared'){ st.turn=false; st.bg={}; st.pend={}; }  }  var bg=false; for(var b in st.bg){ bg=true; break; }  var att=false; for(var p in st.pend){ att=true; break; }  st.run=st.turn||bg; st.att=att;  cnPush();};var OW=window.WebSocket;var live=[];if(OW){  var NW=function(u,p){    try{      if(T&&typeof u==='string'&&u.indexOf('token=')<0){        u=u+(u.indexOf('?')<0?'?':'&')+'token='+encodeURIComponent(T);      }    }catch(e){}    var s=p===undefined?new OW(u):new OW(u,p);    try{      live.push(s);      if(live.length>16) live.shift();      s.addEventListener('close',function(){        for(var i=0;i<live.length;i++){ if(live[i]===s){ live.splice(i,1); break; } }      });      var sid='';      try{ var mm=/\\/ws\\/([^\\/?#]+)/.exec(u); if(mm) sid=decodeURIComponent(mm[1]); }catch(e){}      if(sid){        CN[sid]=(CN[sid]||0)+1;        try{ s.addEventListener('message',function(ev){ cnMsg(sid,ev.data); }); }catch(e){}        s.addEventListener('close',function(){          CN[sid]=(CN[sid]||1)-1;          setTimeout(function(){ if((CN[sid]||0)<=0){ delete S[sid]; cnPush(); } },3000);        });      }    }catch(e){}    return s;  };  NW.prototype=OW.prototype;  NW.CONNECTING=0;NW.OPEN=1;NW.CLOSING=2;NW.CLOSED=3;  window.WebSocket=NW;}window.__cnForceReconnect=function(){  for(var i=live.length-1;i>=0;i--){    try{ live[i].close(); }catch(e){}  }  live.length=0;  for(var k in S){ S[k].pend={}; }  cnPush();};try{if(typeof document!=='undefined'&&document.documentElement){  var RUN=['会话运行中','Session running','會話執行中','セッション実行中','세션 실행 중'];  var ATT=['等待你的授权','Waiting for your approval','等待你的授權','承認待ち','승인 대기 중'];  var any=function(list){    for(var i=0;i<list.length;i++){      try{ if(document.querySelector('[aria-label=\"'+list[i]+'\"]')) return true; }catch(e){}    }    return false;  };  var scan=function(){    var r=false,a=false;    try{ r=any(RUN); a=any(ATT); }catch(e){}    if(!a){ try{ a=!!document.querySelector('[data-testid^=\"tab-strip-attention-\"],[data-testid=\"mobile-attention-dot\"]'); }catch(e){} }    domRun=r; domAtt=a; cnPush();  };  var timer=null,last=0;  var schedule=function(){    if(timer) return;    var w=500-(new Date().getTime()-last); if(w<0) w=0;    timer=setTimeout(function(){timer=null;last=new Date().getTime();scan();},w);  };  var mo=new MutationObserver(schedule);  mo.observe(document.documentElement,{childList:true,subtree:true,attributes:true,attributeFilter:['aria-label','data-testid']});  if(document.readyState==='loading'){ document.addEventListener('DOMContentLoaded',schedule); } else { schedule(); }}}catch(e){}})();</script>";
    }

    private static String jsStr(String str) {
        if (str == null) {
            return "''";
        }
        StringBuilder sb = new StringBuilder("'");
        for (int i = 0; i < str.length(); i++) {
            char charAt = str.charAt(i);
            if (charAt == '\n') {
                sb.append("\\n");
            } else if (charAt == '\r') {
                sb.append("\\r");
            } else if (charAt == '<') {
                sb.append("\\u003c");
            } else if (charAt == '>') {
                sb.append("\\u003e");
            } else if (charAt == '\\') {
                sb.append("\\\\");
            } else if (charAt == '&') {
                sb.append("\\u0026");
            } else if (charAt == '\'') {
                sb.append("\\'");
            } else {
                sb.append(charAt);
            }
        }
        sb.append('\'');
        return sb.toString();
    }

    private static int indexOfIgnoreCase(String str, String str2) {
        int length = str2.length();
        for (int i = 0; i + length <= str.length(); i++) {
            if (str.regionMatches(true, i, str2, 0, length)) {
                return i;
            }
        }
        return -1;
    }
}
