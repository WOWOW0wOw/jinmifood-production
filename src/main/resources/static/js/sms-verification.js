document.addEventListener('DOMContentLoaded',()=>{
  const csrf=document.querySelector('meta[name="_csrf"]')?.content||'';
  document.querySelectorAll('[data-sms-widget]').forEach(widget=>{
    const phone=widget.querySelector('[data-sms-phone]');
    const code=widget.querySelector('[data-sms-code]');
    const send=widget.querySelector('[data-sms-send]');
    const verify=widget.querySelector('[data-sms-verify]');
    const status=widget.querySelector('[data-sms-status]');
    const required=widget.closest('form')?.querySelector('[data-sms-required]');
    const request=async(path,params)=>{
      const response=await fetch(path,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8','X-CSRF-TOKEN':csrf},body:new URLSearchParams(params)});
      const body=await response.json();status.textContent=body.message;status.classList.toggle('success',body.success);return body;
    };
    send?.addEventListener('click',async()=>{send.disabled=true;try{await request('/api/sms/send',{phone:phone.value,purpose:widget.dataset.purpose});}catch(e){status.textContent='문자 발송 중 오류가 발생했습니다.';}finally{setTimeout(()=>{if(!widget.dataset.verifiedPhone)send.disabled=false;},1000);}});
    verify?.addEventListener('click',async()=>{verify.disabled=true;try{const result=await request('/api/sms/verify',{phone:phone.value,purpose:widget.dataset.purpose,code:code.value});if(result.success){widget.dataset.verifiedPhone=phone.value.replace(/[^0-9]/g,'');if(required)required.disabled=false;phone.readOnly=true;code.readOnly=true;send.disabled=true;}}catch(e){status.textContent='인증 확인 중 오류가 발생했습니다.';}finally{if(!widget.dataset.verifiedPhone)verify.disabled=false;}});
    phone?.addEventListener('input',()=>{if(required)required.disabled=true;});
  });
});
