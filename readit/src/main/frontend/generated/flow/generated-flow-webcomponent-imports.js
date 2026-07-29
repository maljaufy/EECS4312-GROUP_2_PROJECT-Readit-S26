import { injectGlobalWebcomponentCss } from 'Frontend/generated/jar-resources/theme-util.js';

import '@vaadin/polymer-legacy-adapter/style-modules.js';
import '@vaadin/text-field/theme/lumo/vaadin-text-field.js';
import '@vaadin/tooltip/theme/lumo/vaadin-tooltip.js';
import '@vaadin/password-field/theme/lumo/vaadin-password-field.js';
import '@vaadin/vertical-layout/theme/lumo/vaadin-vertical-layout.js';
import '@vaadin/icons/vaadin-iconset.js';
import '@vaadin/icon/theme/lumo/vaadin-icon.js';
import '@vaadin/button/theme/lumo/vaadin-button.js';
import 'Frontend/generated/jar-resources/disableOnClickFunctions.js';
import '@vaadin/notification/theme/lumo/vaadin-notification.js';
import 'Frontend/generated/jar-resources/flow-component-renderer.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import '@vaadin/vaadin-lumo-styles/sizing.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === '0c4d52338d81bf938fc9bf203caf66cc24cf048246bb4d3675154f14bb31162c') {
    pending.push(import('./chunks/chunk-13a868328892c13bd132a63b94234a9d06e73a42b871317c58cbf24f32b0ef46.js'));
  }
  if (key === '1d78be51d02ca66a82a4d3f840f8e0638a634cbc3918d46941b76d592052818e') {
    pending.push(import('./chunks/chunk-13a868328892c13bd132a63b94234a9d06e73a42b871317c58cbf24f32b0ef46.js'));
  }
  if (key === '623a27cc5f8bfc8b062aafb09ea7c28f6d578100bef86120eaed3c6734ffdcc9') {
    pending.push(import('./chunks/chunk-f68f622513375c6e0b3c15dabce48760924ad2a7ad2571666ca36a5710c6e6e7.js'));
  }
  if (key === 'a530d0e41dc3ac6ed6cbe605c29ae9d31678eb73341083892d5fef63ce9256de') {
    pending.push(import('./chunks/chunk-af7af2f8cb480ee7577b5bbcedee232225ec6367698716ab550e6c9e12297f0c.js'));
  }
  if (key === '61eb714edf7f34f17e1785eb05350bd6b5063740a952ac0db503048938a637f4') {
    pending.push(import('./chunks/chunk-af7af2f8cb480ee7577b5bbcedee232225ec6367698716ab550e6c9e12297f0c.js'));
  }
  if (key === '5e3980ccc8c7e4c4b19a9a6118a734b68b2afb8374bd110200668184a99b685b') {
    pending.push(import('./chunks/chunk-59824bed99a3e946b07334acd460936b962ad43beb2eb9801f5c67f06c47c97a.js'));
  }
  if (key === 'ea71763f84bf5cc16103e37d92bc80f92ad806cdfd84ad6e2f6143a91049a06e') {
    pending.push(import('./chunks/chunk-aa8a24cef81d373aa521004a87d5444ffcdb37fbceb17cc2ab6f7dbcb81d0411.js'));
  }
  if (key === '2359042729a585e28afceb6369fa56be268fc82c722d8eec1f1281eda95ea316') {
    pending.push(import('./chunks/chunk-9044d31b4546e9187b4fb4cd76754ee1aae1d87c1f46d2b3d03767bedb109e2d.js'));
  }
  if (key === '505905bf98b9f35229363c343f63515f6f04aab5a6cf3478c0bae167000ce040') {
    pending.push(import('./chunks/chunk-13a868328892c13bd132a63b94234a9d06e73a42b871317c58cbf24f32b0ef46.js'));
  }
  if (key === '4376038aacaae9bb34574a5d28bc9f49e4104dcce1eaa7c62acefc9c7094ec59') {
    pending.push(import('./chunks/chunk-13a868328892c13bd132a63b94234a9d06e73a42b871317c58cbf24f32b0ef46.js'));
  }
  if (key === '65b440c50e47b455acdbb1433e1af9b5ac90c0a8942e2b180c648aa9a6c8f5b6') {
    pending.push(import('./chunks/chunk-3b538f812c2ddfd32c5c44163cc410b1d5ea75878f7add87c0cbb8a7def3393f.js'));
  }
  if (key === 'a958e31df4f1679abcd2e8fab6ce249e310d1a964954b76812e79cef4305fe19') {
    pending.push(import('./chunks/chunk-13a868328892c13bd132a63b94234a9d06e73a42b871317c58cbf24f32b0ef46.js'));
  }
  if (key === '94ac857f69d8549f76d036a7c9d934423ce010786c6679fa22e25b94c93345f0') {
    pending.push(import('./chunks/chunk-9795fc09d1e10790bde406e451df0916795866c89dd839da837e72d5b4be0a29.js'));
  }
  if (key === '1e7860e3ac6e694b5a76b28eda414d07f4c230a7b64e0c906a32a6cc0ef044e3') {
    pending.push(import('./chunks/chunk-13a868328892c13bd132a63b94234a9d06e73a42b871317c58cbf24f32b0ef46.js'));
  }
  return Promise.all(pending);
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}