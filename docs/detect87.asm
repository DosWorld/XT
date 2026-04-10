; Method 1 - 8087 detection via control word
; NASM COM file
bits 16
org 0x100

start:
        ; Initialize sentinel
        mov word [cw_save], 0x5A5A

        ; FNINIT (no wait)
        db 0xDB, 0xE3

        ; FSTCW [cw_save]
        db 0xD9, 0x3E
        dw cw_save

        ; FWAIT
        db 0x9B

        mov ax, [cw_save]
        and ax, 0x103F
        cmp ax, 0x003F
        jne no_fpu

        ; FPU present - print "8087 detected"
        mov dx, msg_yes
        mov ah, 0x09
        int 0x21
        jmp done

no_fpu:
        ; No FPU - print "No 8087"
        mov dx, msg_no
        mov ah, 0x09
        int 0x21

done:
        ; Print cw value for debugging
        mov ax, [cw_save]
        call print_hex
        mov dx, newline
        mov ah, 0x09
        int 0x21

        mov ax, 0x4C00
        int 0x21

print_hex:
        ; Print AX as 4 hex digits
        push ax
        push cx
        push dx
        mov cx, 4
.loop:
        rol ax, 1
        rol ax, 1
        rol ax, 1
        rol ax, 1
        mov dl, al
        and dl, 0x0F
        add dl, '0'
        cmp dl, '9'
        jbe .digit
        add dl, 7
.digit:
        mov ah, 0x02
        int 0x21
        loop .loop
        pop dx
        pop cx
        pop ax
        ret

cw_save: dw 0
msg_yes: db '8087 detected$'
msg_no:  db 'No 8087$'
newline: db 13, 10, '$'
