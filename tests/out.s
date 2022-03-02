.data
_prompt: .asciiz "Enter an integer:"
_ret: .asciiz "\n"

.globl main
.text

read:
    li $v0, 4
    la $a0, _prompt
    syscall
    li $v0, 5
    syscall
    jr $ra

write:
    lw $a0, 8($sp)
    li $v0, 1
    syscall
    li $v0, 4
    la $a0, _ret
    syscall
    move $v0, $0
    jr $ra


    # START FUNCTION `getOverallScore`
    # score, 4
    # t8, 8
    # t7, 12
    # t9, 16
    # t10, 20
    # t11, 24
    # t5, 28
    # t13, 32
    # t12, 36
    # t14, 40
    # t15, 44
    # t16, 48
    # t6, 52
    # t3, 56
    # t18, 60
    # t17, 64
    # t19, 68
    # t20, 72
    # t21, 76
    # t4, 80
    # t1, 84
    # t23, 88
    # t22, 92
    # t24, 96
    # t25, 100
    # t26, 104
    # t2, 108
    # t0, 112
getOverallScore:
    move $fp, $sp
    addi $sp, $sp, -112
    lw $t0, 8($fp)
    sw $t0, -4($fp)
    lw $t0, -4($fp)
    sw $t0, -8($fp)
    lw $t0, -8($fp)
    addi $t2, $t0, 0
    sw $t2, -12($fp)
    li $t0, 0
    sw $t0, -16($fp)
    lw $t0, -16($fp)
    li $t1, 4
    mul $t2, $t0, $t1
    sw $t2, -20($fp)
    lw $t0, -12($fp)
    lw $t1, -20($fp)
    add $t2, $t0, $t1
    sw $t2, -24($fp)
    lw $t0, -24($fp)
    lw $t1, 0($t0)
    sw $t1, -28($fp)
    lw $t0, -4($fp)
    sw $t0, -32($fp)
    lw $t0, -32($fp)
    addi $t2, $t0, 0
    sw $t2, -36($fp)
    li $t0, 1
    sw $t0, -40($fp)
    lw $t0, -40($fp)
    li $t1, 4
    mul $t2, $t0, $t1
    sw $t2, -44($fp)
    lw $t0, -36($fp)
    lw $t1, -44($fp)
    add $t2, $t0, $t1
    sw $t2, -48($fp)
    lw $t0, -48($fp)
    lw $t1, 0($t0)
    sw $t1, -52($fp)
    lw $t0, -28($fp)
    lw $t1, -52($fp)
    add $t2, $t0, $t1
    sw $t2, -56($fp)
    lw $t0, -4($fp)
    sw $t0, -60($fp)
    lw $t0, -60($fp)
    addi $t2, $t0, 8
    sw $t2, -64($fp)
    li $t0, 0
    sw $t0, -68($fp)
    lw $t0, -68($fp)
    li $t1, 4
    mul $t2, $t0, $t1
    sw $t2, -72($fp)
    lw $t0, -64($fp)
    lw $t1, -72($fp)
    add $t2, $t0, $t1
    sw $t2, -76($fp)
    lw $t0, -76($fp)
    lw $t1, 0($t0)
    sw $t1, -80($fp)
    lw $t0, -56($fp)
    lw $t1, -80($fp)
    add $t2, $t0, $t1
    sw $t2, -84($fp)
    lw $t0, -4($fp)
    sw $t0, -88($fp)
    lw $t0, -88($fp)
    addi $t2, $t0, 8
    sw $t2, -92($fp)
    li $t0, 1
    sw $t0, -96($fp)
    lw $t0, -96($fp)
    li $t1, 4
    mul $t2, $t0, $t1
    sw $t2, -100($fp)
    lw $t0, -92($fp)
    lw $t1, -100($fp)
    add $t2, $t0, $t1
    sw $t2, -104($fp)
    lw $t0, -104($fp)
    lw $t1, 0($t0)
    sw $t1, -108($fp)
    lw $t0, -84($fp)
    lw $t1, -108($fp)
    add $t2, $t0, $t1
    sw $t2, -112($fp)
    lw $t0, -112($fp)
    move $v0, $t0
    move $sp, $fp
    jr $ra

    # START FUNCTION `main`
    # s, 16
    # t29, 20
    # t28, 24
    # t30, 28
    # t31, 32
    # t27, 36
    # t32, 40
    # t35, 44
    # t34, 48
    # t36, 52
    # t37, 56
    # t33, 60
    # t38, 64
    # t41, 68
    # t40, 72
    # t42, 76
    # t43, 80
    # t39, 84
    # t44, 88
    # t47, 92
    # t46, 96
    # t48, 100
    # t49, 104
    # t45, 108
    # t50, 112
    # t53, 116
    # t52, 120
    # res, 124
    # t55, 128
    # t56, 132
main:
    move $fp, $sp
    addi $sp, $sp, -132
    addi $t0, $fp, -16
    sw $t0, -20($fp)
    lw $t0, -20($fp)
    addi $t2, $t0, 0
    sw $t2, -24($fp)
    li $t0, 0
    sw $t0, -28($fp)
    lw $t0, -28($fp)
    li $t1, 4
    mul $t2, $t0, $t1
    sw $t2, -32($fp)
    lw $t0, -24($fp)
    lw $t1, -32($fp)
    add $t2, $t0, $t1
    sw $t2, -36($fp)
    addi $sp, $sp, -8
    sw $ra, 0($sp)
    sw $fp, 4($sp)
    jal read
    lw $fp, 4($sp)
    lw $ra, 0($sp)
    addi $sp, $sp, 8
    move $t0, $v0
    sw $t0, -40($fp)
    lw $t0, -36($fp)
    lw $t1, -40($fp)
    sw $t1, 0($t0)
    addi $t0, $fp, -16
    sw $t0, -44($fp)
    lw $t0, -44($fp)
    addi $t2, $t0, 0
    sw $t2, -48($fp)
    li $t0, 1
    sw $t0, -52($fp)
    lw $t0, -52($fp)
    li $t1, 4
    mul $t2, $t0, $t1
    sw $t2, -56($fp)
    lw $t0, -48($fp)
    lw $t1, -56($fp)
    add $t2, $t0, $t1
    sw $t2, -60($fp)
    addi $sp, $sp, -8
    sw $ra, 0($sp)
    sw $fp, 4($sp)
    jal read
    lw $fp, 4($sp)
    lw $ra, 0($sp)
    addi $sp, $sp, 8
    move $t0, $v0
    sw $t0, -64($fp)
    lw $t0, -60($fp)
    lw $t1, -64($fp)
    sw $t1, 0($t0)
    addi $t0, $fp, -16
    sw $t0, -68($fp)
    lw $t0, -68($fp)
    addi $t2, $t0, 8
    sw $t2, -72($fp)
    li $t0, 0
    sw $t0, -76($fp)
    lw $t0, -76($fp)
    li $t1, 4
    mul $t2, $t0, $t1
    sw $t2, -80($fp)
    lw $t0, -72($fp)
    lw $t1, -80($fp)
    add $t2, $t0, $t1
    sw $t2, -84($fp)
    addi $sp, $sp, -8
    sw $ra, 0($sp)
    sw $fp, 4($sp)
    jal read
    lw $fp, 4($sp)
    lw $ra, 0($sp)
    addi $sp, $sp, 8
    move $t0, $v0
    sw $t0, -88($fp)
    lw $t0, -84($fp)
    lw $t1, -88($fp)
    sw $t1, 0($t0)
    addi $t0, $fp, -16
    sw $t0, -92($fp)
    lw $t0, -92($fp)
    addi $t2, $t0, 8
    sw $t2, -96($fp)
    li $t0, 1
    sw $t0, -100($fp)
    lw $t0, -100($fp)
    li $t1, 4
    mul $t2, $t0, $t1
    sw $t2, -104($fp)
    lw $t0, -96($fp)
    lw $t1, -104($fp)
    add $t2, $t0, $t1
    sw $t2, -108($fp)
    addi $sp, $sp, -8
    sw $ra, 0($sp)
    sw $fp, 4($sp)
    jal read
    lw $fp, 4($sp)
    lw $ra, 0($sp)
    addi $sp, $sp, 8
    move $t0, $v0
    sw $t0, -112($fp)
    lw $t0, -108($fp)
    lw $t1, -112($fp)
    sw $t1, 0($t0)
    addi $t0, $fp, -16
    sw $t0, -116($fp)
    addi $sp, $sp, -4
    lw $t0, -116($fp)
    sw $t0, 0($sp)
    addi $sp, $sp, -8
    sw $ra, 0($sp)
    sw $fp, 4($sp)
    jal getOverallScore
    lw $fp, 4($sp)
    lw $ra, 0($sp)
    addi $sp, $sp, 12
    move $t0, $v0
    sw $t0, -120($fp)
    lw $t0, -120($fp)
    sw $t0, -124($fp)
    lw $t0, -124($fp)
    sw $t0, -128($fp)
    addi $sp, $sp, -4
    lw $t0, -128($fp)
    sw $t0, 0($sp)
    addi $sp, $sp, -8
    sw $ra, 0($sp)
    sw $fp, 4($sp)
    jal write
    lw $fp, 4($sp)
    lw $ra, 0($sp)
    addi $sp, $sp, 12
    li $t0, 0
    sw $t0, -132($fp)
    lw $t0, -132($fp)
    move $v0, $t0
    move $sp, $fp
    jr $ra
