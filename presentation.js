import {t} from './i18n.js?v=4.0.0';
// Presentation only: no transport, camera or speech side effects.
export function talkPresentation({phase='idle',dictating=false,hasText=false,detail='',flash=false,torchReady=true,torchBusy=false,count=0}={}){
 if(phase==='countdown')return{title:t`Empieza en ${count}…`,hint:t('Orienta los móviles. Puedes cancelar antes de emitir.'),kind:'active',icon:'◷'};
 if(phase==='sending')return{title:t('Enviando luz'),hint:t('Mantén los móviles quietos hasta terminar.'),kind:'active',icon:'↗'};
 if(phase==='waiting')return{title:t('Buscando confirmación por luz'),hint:t('Tu cámara debe ver la luz de vuelta.'),kind:'active',icon:'◉'};
 if(phase==='confirmed')return{title:dictating?t('Mensaje recibido · dictando'):t('Mensaje recibido por el otro móvil'),hint:detail||t('Confirmación verificada. Puedes preparar otro mensaje.'),kind:'success',icon:'✓'};
 if(phase==='unconfirmed')return{title:t('No se ha recibido confirmación'),hint:detail||t('Puedes repetir el mensaje o preparar otro.'),kind:'notice',icon:'?'};
 if(phase==='emitted')return{title:t('Luz emitida · recepción sin confirmar'),hint:t('Comprueba el texto en el otro móvil.'),kind:'notice',icon:'↗'};
 if(phase==='failed'||phase==='stopped')return{title:phase==='failed'?t('No se completó la emisión'):t('Envío detenido'),hint:detail||t('El texto se conserva. Puedes volver a intentarlo.'),kind:'notice',icon:'!'};
 if(dictating)return{title:t('Te escucho · dictando'),hint:t('Pulsa Terminar dictado para revisar y enviar.'),kind:'active',icon:'●'};
 if(flash&&torchBusy)return{title:t('Preparando el flash'),hint:t('Permite el acceso a la cámara cuando se solicite.'),kind:'active',icon:'◷'};
 if(flash&&!torchReady)return{title:t('Flash no preparado'),hint:t('Pulsa Reintentar o selecciona Pantalla.'),kind:'notice',icon:'!'};
 return{title:hasText?t('Mensaje preparado'):t('Prepara tu mensaje'),hint:hasText?t('Revisa el texto y pulsa Enviar luz.'):t('Escribe o pulsa Dictar.'),kind:'idle',icon:hasText?'↗':'○'};
}
export function deliveryLabel(entry){return entry.direction==='in'?t('Texto recibido'):entry.delivery==='confirmed'?t('Recepción confirmada'):entry.delivery==='no-confirmation'?t('Luz emitida · sin confirmación'):t('Luz emitida · recepción sin confirmar');}
