// DOM 요소
const messagesContainer = document.getElementById('messages');
const messageInput = document.getElementById('message');
const filesInput = document.getElementById('files');
const attachments = document.getElementById('attachments');
const form = document.getElementById('chat-form');
const clearButton = document.getElementById('clear-chat');
const dropZone = document.getElementById('drop-zone');

// 태그, 클래스, 텍스트를 받아 공통 DOM 요소를 생성합니다.
function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
}

// 선택된 첨부 파일 목록을 화면에 배지 형태로 렌더링합니다.
function renderAttachmentList(files) {
    attachments.innerHTML = '';
    if (!files || files.length === 0) return;

    Array.from(files).forEach((file) => {
        const chip = el('span', 'attachment', `${file.name} (${Math.max(1, Math.ceil(file.size / 1024))}KB)`);
        attachments.appendChild(chip);
    });
}

// 드래그 앤 드롭으로 받은 파일 목록을 input 요소에 반영합니다.
function setFilesFromFileList(fileList) {
    const dataTransfer = new DataTransfer();
    Array.from(fileList || []).forEach((file) => dataTransfer.items.add(file));
    filesInput.files = dataTransfer.files;
    renderAttachmentList(filesInput.files);
}

// 채팅 메시지 한 건을 사용자 또는 Assistant 버블로 추가합니다.
function appendMessage(role, text) {
    const row = el('div', `message ${role}`);
    const meta = el('div', 'meta', role === 'user' ? '나' : 'Assistant');
    const content = el('div', 'content', text || '');

    row.appendChild(meta);
    row.appendChild(content);
    messagesContainer.appendChild(row);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
    return row;
}

// 대화창 최초 상태에 안내 메시지를 출력합니다.
function appendWelcomeMessage() {
    appendMessage('assistant', '안녕하세요. 메시지와 파일을 올리고 대화를 시작해 주세요.');
}

// 입력창의 메시지를 앞뒤 공백 없이 반환합니다.
function getMessageText() {
    return messageInput.value.trim();
}

// 메시지 입력값과 첨부 파일 표시를 초기화합니다.
function clearInputs() {
    messageInput.value = '';
    filesInput.value = '';
    attachments.innerHTML = '';
}

// 메시지와 첨부 파일을 서버로 전송하고 응답을 채팅창에 반영합니다.
async function sendMessage(event) {
    event.preventDefault();

    const userMessage = getMessageText();
    if (!userMessage) return;

    const files = filesInput.files;

    const userText = files && files.length > 0
        ? `${userMessage}\n\n[첨부 파일]\n${Array.from(files).map((f) => `- ${f.name}`).join('\n')}`
        : userMessage;

    appendMessage('user', userText);
    const assistantBubble = appendMessage('assistant', '생각 중...');
    assistantBubble.classList.add('typing');

    const formData = new FormData();
    formData.append('message', userMessage);
    if (files && files.length > 0) {
        Array.from(files).forEach((file) => formData.append('fileList', file));
    }

    const sendBtn = document.getElementById('send-btn');
    const prevText = sendBtn.textContent;
    sendBtn.disabled = true;
    sendBtn.textContent = '전송 중';

    try {
        const response = await fetch('/api/chat', {
            method: 'POST',
            body: formData,
        });

        if (!response.ok) {
            const errorText = await response.text();
            assistantBubble.querySelector('.content').textContent = `요청 실패: ${response.status} ${response.statusText}\n${errorText}`;
            return;
        }

        const data = await response.json();
        assistantBubble.querySelector('.content').textContent = data.message || '응답이 비어 있습니다.';
    } catch (error) {
        assistantBubble.querySelector('.content').textContent = `네트워크 오류: ${error.message}`;
    } finally {
        assistantBubble.classList.remove('typing');
        sendBtn.disabled = false;
        sendBtn.textContent = prevText;
        clearInputs();
    }
}

// 전체 대화 내용을 비우고 기본 안내 메시지를 다시 표시합니다.
function clearChat() {
    messagesContainer.innerHTML = '';
    appendWelcomeMessage();
}

// ===== 스크립트 실행 시작 =====

// 이벤트 리스너를 등록합니다.
filesInput.addEventListener('change', () => renderAttachmentList(filesInput.files));
dropZone.addEventListener('click', () => filesInput.click());

['dragenter', 'dragover'].forEach((eventName) => {
    dropZone.addEventListener(eventName, (event) => {
        event.preventDefault();
        event.stopPropagation();
        dropZone.classList.add('is-dragover');
    });
});

['dragleave', 'drop'].forEach((eventName) => {
    dropZone.addEventListener(eventName, (event) => {
        event.preventDefault();
        event.stopPropagation();
        dropZone.classList.remove('is-dragover');
    });
});

dropZone.addEventListener('drop', (event) => {
    const droppedFiles = event.dataTransfer && event.dataTransfer.files;
    if (!droppedFiles || droppedFiles.length === 0) return;
    setFilesFromFileList(droppedFiles);
});

messageInput.addEventListener('keydown', (event) => {
    if (event.key !== 'Enter' || event.isComposing) return;
    if (event.shiftKey) return;
    event.preventDefault();
    form.requestSubmit();
});

form.addEventListener('submit', sendMessage);
clearButton.addEventListener('click', clearChat);

// 초기 화면을 구성합니다.
appendWelcomeMessage();
