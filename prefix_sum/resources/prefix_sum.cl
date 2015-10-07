#define WORK_GROUP_SIZE 256

int get_count(int size, int offset)
{
    return size / (2 * offset);
}

int get_index(int id, int offset)
{
    return offset * (2 * id + 1) - 1;
}

void add(int id, __global float * result, int size, int offset, int swap)
{
    int i = get_index(id, offset);
    int j = i + offset;
    if (j < size)
    {
        if (swap)
        {
            float temp = result[i];
            result[i] = result[j];
            result[j] += temp;
        }
        else
        {
            result[j] += result[i];
        }
    }
}

__kernel void reduce(__global float * result, int size, int offset)
{
    int id = get_global_id(0);

    if (get_count(size, offset) > WORK_GROUP_SIZE)
    {
        add(id, result, size, offset, 0);
    }
    else
    {
        while (get_count(size, offset) > 0)
        {
            barrier(CLK_GLOBAL_MEM_FENCE);
            add(id, result, size, offset, 0);
            offset <<= 1;
        }
    }
}

__kernel void sweep(__global float * result, int size, int offset)
{
    int id = get_global_id(0);
    if (id == 0 && offset == size / 2)
    {
        result[size - 1] = 0;
    }

    if (get_count(size, offset) > WORK_GROUP_SIZE)
    {
        add(id, result, size, offset, 1);
    }
    else
    {
        while (offset > 0 && get_count(size, offset) <= WORK_GROUP_SIZE)
        {
            barrier(CLK_GLOBAL_MEM_FENCE);
            add(id, result, size, offset, 1);
            offset >>= 1;
        }
    }
}