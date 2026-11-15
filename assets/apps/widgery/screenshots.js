(()=>{'use strict';
 const dialog=document.getElementById('widgery-shot-dialog');if(!dialog)return;
 const links=[...document.querySelectorAll('[data-widgery-shot]')],image=document.getElementById('widgery-shot-full'),scroller=dialog.querySelector('.widgery-shot-scroll'),title=document.getElementById('widgery-shot-title'),previous=document.getElementById('widgery-shot-prev'),next=document.getElementById('widgery-shot-next'),zoom=document.getElementById('widgery-shot-zoom');
 let index=0,trigger=null,enlarged=false;
 const words=()=>COPY[document.documentElement.lang]||COPY.en;
 function translate(){const t=words();document.querySelectorAll('img[data-shot-caption]').forEach(img=>img.alt='Widgery · '+t[img.dataset.shotCaption]);if(dialog.open){title.textContent=t['shot'+index];image.alt='Widgery · '+title.textContent;}zoom.textContent=t[enlarged?'shotReset':'shotZoom'];}
 function resetZoom(){enlarged=false;scroller.classList.remove('zoomed');image.style.width='';scroller.scrollTop=0;scroller.scrollLeft=0;translate();}
 function display(n){index=n;image.src=links[index].href;resetZoom();previous.disabled=index===0;next.disabled=index===links.length-1;document.getElementById('widgery-shot-count').textContent=(index+1)+' / '+links.length;translate();}
 links.forEach((link,i)=>link.addEventListener('click',event=>{if(event.ctrlKey||event.metaKey||event.shiftKey||event.altKey||event.button!==0)return;event.preventDefault();trigger=link;display(i);dialog.showModal();translate();document.getElementById('widgery-shot-close').focus();}));
 document.getElementById('widgery-shot-close').addEventListener('click',()=>dialog.close());
 dialog.addEventListener('close',()=>{resetZoom();trigger?.focus();});
 previous.addEventListener('click',()=>{if(index>0)display(index-1);});next.addEventListener('click',()=>{if(index<links.length-1)display(index+1);});
 zoom.addEventListener('click',()=>{enlarged=!enlarged;scroller.classList.toggle('zoomed',enlarged);image.style.width=enlarged?Math.round(Math.max(image.naturalWidth||0,640)*1.6)+'px':'';scroller.scrollTop=0;scroller.scrollLeft=0;translate();});
 dialog.addEventListener('keydown',event=>{if(event.key==='ArrowLeft'&&index>0){event.preventDefault();display(index-1);}if(event.key==='ArrowRight'&&index<links.length-1){event.preventDefault();display(index+1);}});
 document.getElementById('lang').addEventListener('change',translate);translate();
})();
